package de.hpi.tdgt.test.story

import com.fasterxml.jackson.annotation.JsonIgnore
import de.hpi.tdgt.test.Test
import de.hpi.tdgt.test.Test.ActiveInstancesThrottler
import de.hpi.tdgt.test.ThreadRecycler
import de.hpi.tdgt.test.story.atom.Atom
import de.hpi.tdgt.test.story.atom.WarmupEnd
import de.hpi.tdgt.util.PropertiesReader
import io.netty.util.HashedWheelTimer
import kotlinx.coroutines.*
import org.apache.logging.log4j.LogManager
import org.asynchttpclient.DefaultAsyncHttpClientConfig
import org.asynchttpclient.Dsl
import org.asynchttpclient.netty.channel.DefaultChannelPool
import java.lang.Runnable
import java.util.*
import java.util.concurrent.ExecutionException


class UserStory : Cloneable {
    var scalePercentage = 0.0
    private var atoms: Array<Atom> = arrayOf()
    var name: String? = null

    /**
     * Holds connections to be shared for every atom
     */
    @JsonIgnore
    val pool= DefaultChannelPool(atoms.size,-1, DefaultChannelPool.PoolLeaseStrategy.FIFO, timer, 1000)

    /**
     * Client that represents this user
     */
    @JsonIgnore
    val client = Dsl.asyncHttpClient(DefaultAsyncHttpClientConfig.Builder().setConnectTimeout(60000).setReadTimeout(120000).setFollowRedirect(true).setKeepAlive(true).setChannelPool(pool).setNettyTimer(timer))
    @JsonIgnore
    var parent: Test? = null

    fun setAtoms(atoms: Array<Atom>) {
        this.atoms = atoms
        //set links
        Arrays.stream(atoms)
            .forEach({ atom: Atom -> atom.initSuccessors(this) })
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
        //fix references
        Arrays.stream(story.getAtoms())
            .forEach { atom: Atom -> atom.initSuccessors(story) }
        return story
    }

    suspend fun run() = coroutineScope<Unit>() {
        val storyRunnable = Runnable {
            runBlocking {
                runAtoms()
            }
        }
        try {
            if(!PropertiesReader.AsyncIO()) {
                ThreadRecycler.instance.executorService.submit(storyRunnable).get()
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

    private suspend fun CoroutineScope.runAtoms() {
        try { //get one of the tickets
            if (ActiveInstancesThrottler.instance != null) {
                try {
                    ActiveInstancesThrottler.instance!!.allowInstanceToRun()
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
                GlobalScope.async{
                    clone.getAtoms()[0].run(HashMap())
                }.await()
            } catch (e: ExecutionException) {
                log.error(e)
            }
            log.info("Finished story " + clone.name.toString() + " in thread " + Thread.currentThread().id)
        } catch (e: InterruptedException) {
            log.error(e)
        }
        finally {
            //free resources
            client.close()
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

    protected fun finalize() {
        client.close()
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
}