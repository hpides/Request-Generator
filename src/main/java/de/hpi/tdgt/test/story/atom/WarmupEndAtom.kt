package de.hpi.tdgt.test.story.atom

import de.hpi.tdgt.concurrency.Event
import kotlinx.coroutines.sync.Semaphore
import org.apache.logging.log4j.LogManager

/**
 * This atom signals the end of the warmup phase.
 * There can be multiple in a test suite, but they have to be in parallel branches.
 */
class WarmupEndAtom : Atom() {
    @Throws(InterruptedException::class)
    override suspend fun perform() {
        addWaiter()
        Event.waitFor(eventName)
    }

    override fun performClone(): Atom {
        return WarmupEndAtom()
    }


    companion object {
        public const val eventName = "warmupEnd";
        private val log =
            LogManager.getLogger(WarmupEndAtom::class.java)

        var waiting = 0
            private set


        private val mutex = Semaphore(1)

        private suspend fun addWaiter() {
            //need coroutine-aware synchronization method here
            mutex.acquire()
            log.info("Added a waiter to the existing $waiting waiters!")
            waiting++
            mutex.release()
        }

        /**
         * Let all waiting warmup ends continue.
         */
        suspend fun startTest() {
            log.info("START TEST!")
            Event.signal(eventName)
            waiting = 0
        }

    }
}
