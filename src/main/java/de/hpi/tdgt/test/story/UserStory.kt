package de.hpi.tdgt.test.story

import com.fasterxml.jackson.annotation.JsonIgnore
import de.hpi.tdgt.test.Test
import de.hpi.tdgt.test.Test.ActiveInstancesThrottler
import de.hpi.tdgt.test.ThreadRecycler
import de.hpi.tdgt.test.story.atom.Atom
import de.hpi.tdgt.test.story.atom.WarmupEnd
import org.apache.logging.log4j.LogManager
import java.util.*
import java.util.concurrent.ExecutionException
import java.util.function.Consumer


class UserStory : Runnable, Cloneable {
    var scalePercentage = 0.0
    var name: String? = null
    private var atoms: Array<Atom> = arrayOf()
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

    override fun run() {
        val storyRunnable = Runnable {
            try { //get one of the tickets
                if (ActiveInstancesThrottler.instance != null) {
                    try {
                        ActiveInstancesThrottler.instance!!.allowInstanceToRun()
                    } catch (e: InterruptedException) {
                        log.error("Interrupted wail waiting to be allowed to send a request, aborting: ", e)
                        return@Runnable
                    }
                } else {
                    log.warn("Internal error: Can not limit active story instances per second!")
                }
                var clone: UserStory
                synchronized(this) { clone = clone() }
                log.info("Running story " + clone.name.toString() + " in thread " + Thread.currentThread().id)
                try {
                    clone.getAtoms()[0].run(HashMap<String, String>())
                } catch (e: ExecutionException) {
                    log.error(e)
                }
                log.info("Finished story " + clone.name.toString() + " in thread " + Thread.currentThread().id)
            } catch (e: InterruptedException) {
                log.error(e)
            }
        }
        try {
            ThreadRecycler.instance.executorService.submit(storyRunnable).get()
        } catch (e: InterruptedException) {
            log.error(e)
        } catch (e: ExecutionException) {
            log.error(e)
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

    companion object {
        private val log =
            LogManager.getLogger(UserStory::class.java)
    }
}