package de.hpi.tdgt.test

import de.hpi.tdgt.test.Test.ActiveInstancesThrottler
import de.hpi.tdgt.test.story.UserStory
import de.hpi.tdgt.test.story.atom.Data_Generation
import de.hpi.tdgt.test.story.atom.WarmupEnd
import de.hpi.tdgt.util.PropertiesReader
import jdk.jshell.spi.ExecutionControl
import org.apache.catalina.User
import org.apache.logging.log4j.LogManager
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttException
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.concurrent.ExecutionException
import java.util.concurrent.Future
import java.util.concurrent.Semaphore
import java.util.function.Consumer
import java.util.function.Predicate
import java.util.function.ToIntFunction
import java.util.stream.Collectors

class Test {
    //stories need to know the test e.g. for it's id
    fun setStories(stories: Array<UserStory?>) {
        this.stories = stories
        stories.filterNotNull().forEach { story: UserStory -> story.parent = this  }
    }

    /**
     * Contains the original test config as JSON. This saves time because when broadcasting it, the test does not have to be serialized again.
     */
    var configJSON: String? = null
    var repeat = 0
    var scaleFactor = 0
    private var stories: Array<UserStory?> = arrayOf()
    //this is used to be able to repeat them
    var stories_clone: Array<UserStory?> = arrayOf()
    var activeInstancesPerSecond =
        DEFAULT_ACTIVE_INSTANCES_PER_SECOND_LIMIT
    var client: MqttClient? = null
    //by default, do not limit number of concurrent requests
    var maximumConcurrentRequests = DEFAULT_CONCURRENT_REQUEST_LIMIT
    //we can assume this is unique. Probably, only one test at a time is run.
    val testId = System.currentTimeMillis()

    /**
     * Perform deep clone of stories to user_stories.
     */
    private fun cloneStories(source: Array<UserStory?>, target: Array<UserStory?>) {
        for (i in source.indices) {
            target[i] = source[i]!!.clone()
        }
    }

    /**
     * Run all stories that have WarmupEnd until reaching WarmupEnd. Other stories are not run.
     * *ALWAYS* run start after running warmup before you run warmup again, even in tests, to get rid of waiting threads.
     * @return threads in which the stories run to join later
     * @throws InterruptedException if interrupted in Thread.sleep
     */
    @Throws(InterruptedException::class)
    fun warmup(): Collection<Future<*>> { //preserve stories for test repetition
        stories_clone = arrayOfNulls(stories.size)
        cloneStories(stories, stories_clone)
        ActiveInstancesThrottler.setInstance(activeInstancesPerSecond)
        val watchdog = Thread(ActiveInstancesThrottler.instance)
        watchdog.priority = Thread.MAX_PRIORITY
        watchdog.start()
        //will run stories with warmup only, so they can run until WarmupEnd is reached
        val threads =
            runTest(
                stories.filter({story -> story?.hasWarmup()!!}).stream().collect(Collectors.toList()).toTypedArray().requireNoNulls()
            )
        //now, wait for all warmups to finish
//casting to int clears decimals
        val waitersToExpect = stories
            .filterNotNull().stream()
            .mapToInt({ story: UserStory -> (story.numberOfWarmupEnds() * story.scalePercentage * scaleFactor).toInt() })
            .sum()
        //wait for all warmup ends to be stuck
        while (waitersToExpect > WarmupEnd.getWaiting()) {
            log.info("Waiting for warmup to complete: " + WarmupEnd.getWaiting() + " of " + waitersToExpect + " complete!")
            Thread.sleep(5000)
        }
        watchdog.interrupt()
        return threads
    }

    /**
     * Use this if you do not have threads from warmup.
     * @throws InterruptedException if interrupted joining threads
     */
    @Throws(InterruptedException::class, ExecutionException::class)
    fun start() { //preserve stories for repeat
        stories_clone = arrayOfNulls(stories.size)
        cloneStories(stories, stories_clone)
        start(Vector())
    }

    /**
     * Use this method to wait for threads left from warmup.
     * @param threadsFromWarmup Collection of threads to wait for
     * @throws InterruptedException if interrupted joining threads
     */
    @Throws(InterruptedException::class, ExecutionException::class)
    fun start(threadsFromWarmup: Collection<Future<*>>) {
        var threadsFromWarmup = threadsFromWarmup
        prepareMqttClient()
        try { //clear retained messages from last test
            client!!.publish(MQTT_TOPIC, ByteArray(0), 0, true)
            client!!.publish(
                MQTT_TOPIC,
                "testStart $testId $configJSON".toByteArray(StandardCharsets.UTF_8),
                2,
                false
            )
        } catch (e: MqttException) {
            log.error("Could not send control start message: ", e)
        }
        for (i in 0 until repeat) {
            log.info("Starting test run $i of $repeat")
            //start all warmup tasks
            WarmupEnd.startTest()
            //this thread makes sure that requests per second get limited
            ActiveInstancesThrottler.setInstance(activeInstancesPerSecond)
            val watchdog = Thread(ActiveInstancesThrottler.instance)
            watchdog.priority = Thread.MAX_PRIORITY
            watchdog.start()
            val threads = runTest(
                Arrays.stream(stories).filter { story -> story?.isStarted!! }.collect(Collectors.toList()).toTypedArray().requireNoNulls()
            )
            //can wait for these threads also
            threads.addAll(threadsFromWarmup)
            for (thread in threads) { //join thread
                if (!thread.isCancelled) thread.get()
            }
            watchdog.interrupt()
            //remove global state
            ActiveInstancesThrottler.reset()
            Data_Generation.reset()
            //this resets all state atoms might have
            cloneStories(stories_clone, stories)
            //do not run another warmup after the last run, because it would not be finished
            if (i < repeat - 1) {
                threadsFromWarmup = warmup()
            }
        }
        try {
            client!!.publish(
                MQTT_TOPIC,
                "testEnd $testId".toByteArray(StandardCharsets.UTF_8),
                2,
                false
            )
            //clear retained messages for next test
            client!!.publish(MQTT_TOPIC, ByteArray(0), 0, true)
        } catch (e: MqttException) {
            log.error("Could not send control end message: ", e)
        }
        try {
            client!!.disconnect()
        } catch (e: MqttException) {
            log.warn("Could not disconnect client: ", e)
        }
    }

    private fun prepareMqttClient() {
        if (client == null || !client!!.isConnected) {
            try {
                val publisherId = UUID.randomUUID().toString()
                //use memory persistence because it is not important that all packets are transferred and we do not want to spam the file system
                client = MqttClient(PropertiesReader.getMqttHost(), publisherId, MemoryPersistence())
            } catch (e: MqttException) {
                log.error("Error creating mqttclient in AssertionStorage: ", e)
            }
            val options = MqttConnectOptions()
            options.isAutomaticReconnect = true
            options.isCleanSession = true
            options.connectionTimeout = 10
            try {
                client!!.connect(options)
                //clear retained messages from last test
                client!!.publish(MQTT_TOPIC, ByteArray(0), 0, true)
            } catch (e: MqttException) {
                log.error("Could not connect to mqtt broker in AssertionStorage: ", e)
            }
        }
    }

    @Throws(InterruptedException::class)
    private fun runTest(stories: Array<UserStory>): MutableCollection<Future<*>> {
        try {
            ConcurrentRequestsThrottler.instance.setMaxParallelRequests(maximumConcurrentRequests)
        } catch (e: ExecutionControl.NotImplementedException) {
            log.error(e)
        }
        val futures = Vector<Future<*>>()
        for (i in stories.indices) { //repeat stories as often as wished
            var j = 0
            while (j < scaleFactor * stories[i].scalePercentage) {
                stories[i].parent = this
                stories[i].isStarted = true
                val future: Future<Any> =
                    ThreadRecycler.getInstance().executorService.submit(stories[i]) as Future<Any>
                futures.add(future)
                j++
            }
        }
        return futures
    }

    fun getStories(): Array<UserStory?> {
        return stories
    }

    /**
     * This is a runnable for a high-priority thread that makes sure that no more user stories than requested run.
     */
    class ActiveInstancesThrottler private constructor(instancesPerSecond: Int) : Runnable {
        //only used for measurement, does not have to be synchronized
        var instancesPerSecond = 0
        /**
         * Stops threads from increasing requests per second while re-creating tickets
         */
        private val mutex = Semaphore(1)
        private val requestLimiter: Semaphore
        @Throws(InterruptedException::class)
        fun allowInstanceToRun() {
            log.trace("Waiting for requestLimiter...")
            requestLimiter.acquire()
            log.trace("Waiting for mutex (allowRequest)...")
            mutex.acquire()
            instancesPerSecond++
            mutex.release()
            log.trace("Released mutex (allowRequest)")
        }

        override fun run() {
            while (!Thread.interrupted()) {
                val instancesLastSecond = instancesPerSecond
                log.trace("Waiting for mutex (run)...")
                log.info("$instancesLastSecond active instances in the last second.")
                try {
                    mutex.acquire()
                    requestLimiter.release(instancesPerSecond)
                    instancesPerSecond = 0
                    mutex.release()
                    log.trace("Released mutex (run)")
                    Thread.sleep(1000)
                } catch (e: InterruptedException) {
                    return
                }
            }
            log.trace("Requests per second watchdog was interrupted!")
        }

        companion object {
            private val log = LogManager.getLogger(
                ActiveInstancesThrottler::class.java
            )

            fun setInstance(instancesPerSecond: Int) {
                instance = ActiveInstancesThrottler(instancesPerSecond)
            }

            @JvmStatic
            var instance: ActiveInstancesThrottler? = null
                private set

            fun reset() {
                instance = null
            }

        }

        init {
            requestLimiter = Semaphore(instancesPerSecond, true)
        }
    }

    /**
     * This makes sure not more requests than configured run in parallel
     */
    class ConcurrentRequestsThrottler {
        private var maxParallelRequests: Semaphore? = null
        private var waiters = 0
        private var active = 0
        var maximumParallelRequests = 0
        @Throws(ExecutionControl.NotImplementedException::class)
        fun setMaxParallelRequests(concurrent: Int) {
            if (maxParallelRequests == null) {
                maxParallelRequests = Semaphore(concurrent)
                active = 0
                maximumParallelRequests = 0
            } else {
                var waiters: Int
                synchronized(this) {
                    waiters = this.waiters
                    //we needed to wait until no thread is waiting for the semaphore
                    if (waiters > 0) {
                        throw ExecutionControl.NotImplementedException("Can not change number of concurrent requests while there are threads waiting for the semaphore!")
                    }
                    maxParallelRequests = Semaphore(concurrent)
                    active = 0
                    maximumParallelRequests = 0
                }
            }
        }

        @Throws(InterruptedException::class)
        fun allowRequest() {
            synchronized(this) { waiters++ }
            if (maxParallelRequests != null) {
                maxParallelRequests!!.acquire()
            }
            synchronized(this) {
                waiters--
                active++
                //used by the test of this feature
                if (active > maximumParallelRequests) {
                    maximumParallelRequests = active
                }
            }
        }

        fun requestDone() {
            if (maxParallelRequests != null) {
                maxParallelRequests!!.release()
            }
            synchronized(this) { active-- }
        }

        fun reset() {
            maxParallelRequests = null
            active = 0
            maximumParallelRequests = 0
        }

        companion object {
            @JvmStatic
            val instance = ConcurrentRequestsThrottler()
        }
    }

    companion object {
        /**
         * Topic on which control messages are broadcasted
         */
        const val MQTT_TOPIC = "de.hpi.tdgt.control"
        private val log =
            LogManager.getLogger(Test::class.java)
        const val DEFAULT_CONCURRENT_REQUEST_LIMIT = 100
        const val DEFAULT_ACTIVE_INSTANCES_PER_SECOND_LIMIT = 10000
    }
}
