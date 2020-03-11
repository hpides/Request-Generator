package de.hpi.tdgt.test.story.atom

import org.apache.logging.log4j.LogManager
import java.util.concurrent.Semaphore

/**
 * This atom signals the end of the warmup phase.
 * There can be multiple in a test suite, but they have to be in parallel branches.
 */
class WarmupEnd : Atom() {
    @Throws(InterruptedException::class)
    override fun perform() {
        addWaiter()
        warmupEnd.acquire()
    }

    override fun performClone(): Atom {
        return WarmupEnd()
    }

    companion object {
        private val log =
            LogManager.getLogger(WarmupEnd::class.java)

        //Test knows how many warmupEnds should be waiting for this semaphore.
//So when this number is reached, it can release all of them and thus continue processing.
        private val warmupEnd = Semaphore(0)
        var waiting = 0
            private set

        @Synchronized
        private fun addWaiter() {
            log.info("Added a waiter to the existing $waiting waiters!")
            waiting++
        }

        /**
         * Let all waiting warmup ends continue.
         */
        fun startTest() {
            warmupEnd.release(waiting)
            waiting = 0
        }
    }
}