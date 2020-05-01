package de.hpi.tdgt.test

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import de.hpi.tdgt.concurrency.Event
import de.hpi.tdgt.test.story.UserStory
import de.hpi.tdgt.test.story.atom.Data_Generation
import de.hpi.tdgt.test.story.atom.WarmupEnd
import de.hpi.tdgt.test.story.atom.assertion.AssertionStorage
import de.hpi.tdgt.test.time_measurement.TimeStorage
import de.hpi.tdgt.util.PropertiesReader
import jdk.jshell.spi.ExecutionControl
import org.apache.logging.log4j.LogManager
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttException
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.concurrent.Future
import java.util.stream.Collectors
import kotlinx.coroutines.*
import kotlinx.coroutines.future.asCompletableFuture
import kotlinx.coroutines.future.await
import kotlinx.coroutines.sync.Semaphore
import java.lang.Exception
import java.lang.Long.max
import java.lang.Thread.sleep
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicLong
import kotlin.collections.HashMap

//allow frontend to store additional information
@JsonIgnoreProperties(ignoreUnknown = true)
class Test {
    //stories need to know the test e.g. for it's id
    fun setStories(stories: Array<UserStory>) {
        this.stories = stories
        Arrays.stream(stories)
            .forEach({ story: UserStory -> story.parent = this })
    }
    var nodeNumber = 0L
    var nodes = 1L
    /**
     * Contains the original test config as JSON. This saves time because when broadcasting it, the test does not have to be serialized again.
     */
    var configJSON: String? = null
    var repeat = 0
    var scaleFactor = 0L
    get(){
        //every node should only do a part of the task at hand, but this should not be 0 (else no node does anything)
        return kotlin.math.max(field / nodes,1)
    }

    /**
     * True to enable global connection pooling, else false.
     */
    var noSession = false;
    private var stories: Array<UserStory> = arrayOf()
    //this is used to be able to repeat them
    var stories_clone: Array<UserStory> = arrayOf()
    var activeInstancesPerSecond = DEFAULT_ACTIVE_INSTANCES_PER_SECOND_LIMIT
        get(){
            //every node should only do a part of the task at hand
            return kotlin.math.max(field / nodes.toInt(),1)
        }
    var client: MqttClient? = null
    //by default, do not limit number of concurrent requests
    var maximumConcurrentRequests = DEFAULT_CONCURRENT_REQUEST_LIMIT
        get(){
            //every node should only do a part of the task at hand
            return kotlin.math.max(field / nodes.toInt(),1)
        }
    @JsonIgnore //we can assume this is unique. Probably, only one test at a time is run.
    var testId = System.currentTimeMillis()

    /**
     * Perform deep clone of stories to user_stories.
     */
    private fun cloneStories(source: Array<UserStory>):Array<UserStory> {
        var ret = arrayOf<UserStory>()
        for (i in source.indices) {
            ret += source[i].clone()
        }
        return ret
    }

    /**
     * Run all stories that have WarmupEnd until reaching WarmupEnd. Other stories are not run.
     * *ALWAYS* run start after running warmup before you run warmup again, even in tests, to get rid of waiting threads.
     * @return threads in which the stories run to join later
     */
    suspend fun warmup(): MutableCollection<Future<*>> { //preserve stories for test repetition
        stories_clone = cloneStories(stories)
        ActiveInstancesThrottler.setInstance(activeInstancesPerSecond)
        val watchdog = Thread(ActiveInstancesThrottler.instance)
        watchdog.priority = Thread.MAX_PRIORITY
        watchdog.start()
        Event.unsignal(WarmupEnd.eventName)
        //will run stories with warmup only, so they can run until WarmupEnd is reached
        val threads =
            runTest(
                Arrays.stream(stories).filter { obj: UserStory? -> if(obj!=null){obj.hasWarmup()}else{false} }.collect(Collectors.toList()).toTypedArray()
            )
        //now, wait for all warmups to finish
//casting to int clears decimals
        val waitersToExpect = Arrays.stream(stories)
            .mapToInt { story: UserStory -> (story.numberOfWarmupEnds() * story.scalePercentage * scaleFactor).toInt() }
                .sum()
        //wait for all warmup ends to be stuck
        val future = Thread {
            while (waitersToExpect > WarmupEnd.waiting) {
                log.info("Waiting for warmup to complete: " + WarmupEnd.waiting + " of " + waitersToExpect + " complete!")
                sleep(1000)
            }
        }
        future.start()
        future.join()
        log.info("Warmup complete")
        watchdog.interrupt()
        return threads
    }

    /**
     * Use this if you do not have threads from warmup.
     * @throws InterruptedException if interrupted joining threads
     */
    fun start() { //preserve stories for repeat
        stories_clone = cloneStories(stories)
        start(Vector())
    }

    /**
     * Use this method to wait for threads left from warmup.
     * @param threadsFromWarmup Collection of threads to wait for
     * @throws InterruptedException if interrupted joining threads
     */
    fun start(threadsFromWarmup: MutableCollection<Future<*>>) {
        var threadsFromWarmupReceived = threadsFromWarmup
        prepareMqttClient()
        try { //clear retained messages from last test
            client!!.publish(MQTT_TOPIC, ByteArray(0), 0, true)
            remainingNodesForTest[testId.toString()] = AtomicLong(nodes)
            client!!.publish(
                MQTT_TOPIC,
                "testStart $testId $configJSON".toByteArray(StandardCharsets.UTF_8),
                2,
                false
            )
        } catch (e: MqttException) {
            log.error("Could not send control start message: ", e)
        }
        runBlocking {
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
                        Arrays.stream(stories).filter(
                                { story: UserStory -> !story.isStarted }
                        ).collect(Collectors.toList()).toTypedArray()
                )
                //can wait for these threads also
                threads.addAll(threadsFromWarmupReceived)
                for (thread in threads) { //join thread
                    if(PropertiesReader.AsyncIO() && thread is CompletableFuture){
                        thread.await()
                    }
                    else{
                        if (!thread.isCancelled) thread.get()
                    }
                }
                watchdog.interrupt()
                //remove global state
                ActiveInstancesThrottler.reset()
                Data_Generation.reset()
                //this resets all state atoms might have
                stories = cloneStories(stories_clone)
                //do not run another warmup after the last run, because it would not be finished
                if (i < repeat - 1) {
                    threadsFromWarmupReceived = warmup()
                }

                log.info("Test run $i complete!")
            }
            //make sure all times are sent
            AssertionStorage.instance.flush()
            TimeStorage.instance.flush()
            //if there is only this node, the test is over; else, other nodes might still be running
            val endMessage = (if(nodes == 1L){"testEnd "}else{"nodeEnd $nodeNumber "} +"$testId").toByteArray(StandardCharsets.UTF_8)
            try {
                client!!.publish(
                        MQTT_TOPIC,
                        endMessage,
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

    /**
     * Time when user stories actually start running
     */
    var testStart:Long = 0

    @Throws(InterruptedException::class)
    private suspend fun runTest(stories: Array<UserStory>): MutableCollection<Future<*>> {
        //old testStart should be gone by now
        Event.unsignal(testStartEvent)
        //since only used in some scenarios, it is less shotgun surgery to set it here than to include it in all possible paths through RestClient.
        TimeStorage.instance.nodeNumber = nodeNumber
        AssertionStorage.instance.nodeNumber = nodeNumber
        //mapping all files eagerly might improve performance, also this is needed to set an offset in the data
        Arrays.stream(stories).forEach{ story -> Arrays.stream(story.getAtoms()).forEach { atom -> if(atom is Data_Generation){
            atom.offsetPercentage = nodeNumber.toDouble() / nodes
            runBlocking {
                atom.initScanner()
            }
        } }}
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
                var future: Future<*>?
                if(!PropertiesReader.AsyncIO()) {
                    future = ThreadRecycler.instance.executorService.submit {
                        runBlocking {
                            stories[i].run()
                        }
                    }
                }else{
                    //withContext(Dispatchers.IO) {
                        future = GlobalScope.async { stories[i].run() }.asCompletableFuture()
                    //}
                }
                futures.add(future)
                j++
            }
        }
        //cloning takes considerably much time, so make sure all stories are cloned before they actually start
        Event.signal(testStartEvent)
        testStart = System.currentTimeMillis()
        return futures
    }

    fun getStories(): Array<UserStory> {
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
        suspend fun allowInstanceToRun() {
            log.trace("Waiting for requestLimiter...")
            requestLimiter.acquire()
            log.trace("Waiting for mutex (allowRequest)...")
            mutex.acquire()
            instancesPerSecond++
            mutex.release()
            log.trace("Released mutex (allowRequest)")
        }
        override fun run() {
            try {
                runBlocking {performAction()}
            } catch(e:InterruptedException){
                return
            }
        }
        private suspend fun performAction() {
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
                    delay(1000)
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
            requestLimiter = Semaphore(instancesPerSecond)
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

        suspend fun allowRequest() {
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
                try {
                    maxParallelRequests!!.release()
                } catch (e:Exception){
                    log.error("Could not release permit in concurrentRequestThrottler: ",e)
                }
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
        const val testStartEvent = "testStart"
        @JvmStatic
        val testEndWatchdog : MqttClient =
            MqttClient(PropertiesReader.getMqttHost(), UUID.randomUUID().toString(), MemoryPersistence())
        /**
         * For every test id the number of nodes that have to finish for it to be over
         */
        @JvmStatic
        val remainingNodesForTest: HashMap<String, AtomicLong> = HashMap()
        init {
            val options = MqttConnectOptions()
            options.isAutomaticReconnect = true
            options.isCleanSession = true
            options.connectionTimeout = 10
            try {
                testEndWatchdog.connect(options)
            } catch (e: MqttException) {
                log.error("Could not start testEnd Watchdog ", e)
            }
            testEndWatchdog.subscribe(MQTT_TOPIC){
                topic, message ->  run{
                if(topic == MQTT_TOPIC && message != null) {
                    val text = String(message.payload)
                    if(text.startsWith("nodeEnd")){
                        val parts = text.split(" ");
                        if(parts.size < 3){
                            log.error("TestEnd protocol violation: nodeEnd messages need to be 3 parts separated by spaces!")
                            return@run
                        }
                        val nodeId = parts[1]
                        val testId = parts[2]
                        log.info("Test $testId: Node $nodeId is finished!")
                        if(remainingNodesForTest[testId]?.decrementAndGet() == 0L){
                            log.info("Test $testId finished!")
                            testEndWatchdog.publish(MQTT_TOPIC,"testEnd $testId".toByteArray(),1,false)
                        }
                    }
                }

            }
            }
        }
    }
}

private fun Semaphore.release(instancesPerSecond: Int) {
    for(i in 0 until instancesPerSecond){
        release()
    }
}
