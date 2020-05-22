package de.hpi.tdgt.test.story

import com.fasterxml.jackson.annotation.JsonIgnore
import de.hpi.tdgt.concurrency.Event
import de.hpi.tdgt.test.Test
import de.hpi.tdgt.test.ThreadRecycler
import de.hpi.tdgt.test.story.atom.Atom
import de.hpi.tdgt.test.story.atom.WarmupEnd
import de.hpi.tdgt.util.PropertiesReader
import io.netty.util.HashedWheelTimer
import org.apache.logging.log4j.LogManager
import org.asynchttpclient.AsyncHttpClient
import org.asynchttpclient.DefaultAsyncHttpClientConfig
import org.asynchttpclient.Dsl
import org.asynchttpclient.netty.channel.DefaultChannelPool
import java.lang.Runnable
import java.lang.Thread.sleep
import java.util.*
import java.util.concurrent.ExecutionException
import java.util.concurrent.Semaphore


class UserStory : Cloneable {
    var scalePercentage = 0.0
    private var atoms: Array<Atom> = arrayOf()
    var name: String? = null
    var instancesThrottler : ActiveInstancesThrottler? = null
    var watchdog : Thread? = null
    /**
     * Holds connections to be shared for every atom.
     * Expensive resource, will be initialized only if needed
     */
    @JsonIgnore
    lateinit var pool : DefaultChannelPool
    /**
     * Client that represents this user
     */
    @JsonIgnore
    var client:AsyncHttpClient = staticClient
    @JsonIgnore
    var parent: Test? = null
        set(value){
            field = value
            //Windows resource leakage: One can not create e.g. 100.000 clients, so start off with the assumption only one is wanted and create them if necessary; Event makes sure that this is not counted as test time
            if(value?.noSession == false && client === staticClient){
                pool = DefaultChannelPool(atoms.size,-1, DefaultChannelPool.PoolLeaseStrategy.FIFO, timer, 1000)
                client = Dsl.asyncHttpClient(DefaultAsyncHttpClientConfig.Builder().setConnectTimeout(60000).setReadTimeout(120000).setFollowRedirect(true).setKeepAlive(true).setChannelPool(pool).setNettyTimer(timer))
            }
        }

    fun setAtoms(atoms: Array<Atom>) {
        this.atoms = atoms
        //set links
        Arrays.stream(atoms)
            .forEach { atom: Atom -> atom.initSuccessors(this) }
    }

    public override fun clone(): UserStory {
        val story = UserStory()
        story.name = name
        story.scalePercentage = scalePercentage
        story.atoms = arrayOf()
        //make a clone for all local changes, e.g. predecessorsReady
        for (i in atoms.indices) {
            story.atoms  += atoms[i].clone()
        }
        story.parent = parent
        //by design need to be shared by all cloned instances
        story.instancesThrottler = instancesThrottler
        story.watchdog = watchdog
        //fix references
        Arrays.stream(story.getAtoms())
            .forEach { atom: Atom -> atom.initSuccessors(story) }
        return story
    }

    /**
     * Run at most limit instances of this story per second.
     * If limit is less than 1, 1 instance per second will be run.
     */
    fun limitActiveInstancesPerSecondTo(limit: Int){
        instancesThrottler = if(limit>0 ) {ActiveInstancesThrottler(limit)} else {ActiveInstancesThrottler(1)}
        watchdog = Thread(instancesThrottler)
        //if another thread reset it already, go with it
        watchdog?.priority = Thread.MAX_PRIORITY
        watchdog?.start()
    }

     fun run() {
        val storyRunnable = Runnable {
            runAtoms()
        }
        try {
            if(!PropertiesReader.AsyncIO()) {
                val thread = Thread(storyRunnable)
                thread.start()
                thread.join()
            }
            else{
                runAtoms()
            }
        } catch (e: InterruptedException) {
            log.error(e)
        } catch (e: ExecutionException) {
            log.error(e)
        }
    }

    private  fun runAtoms() {
        try { //get one of the tickets
            if (instancesThrottler!=null) {
                try {
                    //if another thread reset it, go with it
                    instancesThrottler?.allowInstanceToRun()
                } catch (e: InterruptedException) {
                    log.error("Interrupted wail waiting to be allowed to send a request, aborting: ", e)
                    return
                }
            } else {
                log.warn("Internal error: Can not limit active story instances per second!")
            }
            var clone: UserStory
            synchronized(this) { clone = clone() }
            log.info("Running story " + clone.name.toString() + " in thread " + Thread.currentThread().id)
            try {
                    Event.waitFor(Test.testStartEvent)
                    clone.getAtoms()[0].run(HashMap())
            } catch (e: ExecutionException) {
                log.error(e)
            }
            log.info("Finished story " + clone.name.toString() + " in thread " + Thread.currentThread().id)
        } catch (e: InterruptedException) {
            log.error(e)
        }
        finally {
            finalize()
        }
    }

    fun hasWarmup(): Boolean {
        for (atom in atoms) {
            if (atom is WarmupEnd) {
                return true
            }
        }
        return false
    }

    /**
     * True if there are threads that run the activities of this story, false otherwise. Has to be set by Test.
     */
    @JsonIgnore
    var isStarted = false

    /**
     * Returns number of times a warmup in this story is waiting for a mutex.
     * @return int
     */
    fun numberOfWarmupEnds(): Int {
        var warmupEnds = 0
        for (atom in atoms) {
            if (atom is WarmupEnd) {
                warmupEnds += atom.repeat
            }
        }
        return warmupEnds
    }

    fun getAtoms(): Array<Atom> {
        return atoms
    }

    init {
        timer.start()
    }
    //on GC, remember to return ressources to the OS
    protected fun finalize() {
        //free resources, but only if client is not shared
        if(client !== staticClient)client.close()
    }

    companion object {
        private val log =
            LogManager.getLogger(UserStory::class.java)

        /*
        * A netty internum. Only a handful per JVM recommended.
        */
        @JvmStatic
        val timer = HashedWheelTimer()
        /**
         * To be used by the static client. Sharing it with all dynamic clients slows them down, probably because it needs to synchronize accesses.
         */
        val staticPool= DefaultChannelPool(60000,-1, DefaultChannelPool.PoolLeaseStrategy.FIFO, timer, 1000)
        /*
         * Fallback in case no client per story shall be created
         */
        @JvmStatic
        val staticClient = Dsl.asyncHttpClient(DefaultAsyncHttpClientConfig.Builder().setConnectTimeout(60000).setReadTimeout(120000).setFollowRedirect(true).setKeepAlive(true).setChannelPool(staticPool).setNettyTimer(timer))

    }

    /**
     * This is a runnable for a high-priority thread that makes sure that no more user stories than requested run.
     */
    public class ActiveInstancesThrottler constructor(instancesPerSecond: Int) : Runnable {
        //only used for measurement, does not have to be synchronized
        var instancesPerSecond = 0
        /**
         * Stops threads from increasing requests per second while re-creating tickets
         */
        private val mutex = Semaphore(1)
        private val requestLimiter: Semaphore = Semaphore(instancesPerSecond)

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
            try {
                performAction()
            } catch(e:InterruptedException){
                return
            }
        }
        private  fun performAction() {
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
                    sleep(1000)
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

        }

    }
}
private fun Semaphore.release(instancesPerSecond: Int) {
    for(i in 0 until instancesPerSecond){
        release()
    }
}