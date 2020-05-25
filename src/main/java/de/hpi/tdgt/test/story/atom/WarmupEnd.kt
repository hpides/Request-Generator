package de.hpi.tdgt.test.story.atom

import de.hpi.tdgt.concurrency.Event
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import java.util.concurrent.Semaphore
import java.util.concurrent.atomic.AtomicInteger

/**
 * This atom signals the end of the warmup phase.
 * There can be multiple in a test suite, but they have to be in parallel branches.
 */
class WarmupEnd : Atom() {
    @Throws(InterruptedException::class)
    override  fun perform() {
        addWaiter()
        Event.waitFor(eventName)
    }

    override fun performClone(): Atom {
        return WarmupEnd()
    }

    override val log: Logger
        get() = WarmupEnd.log


    companion object {
        public const val eventName = "warmupEnd";
        private val log =
            LogManager.getLogger(WarmupEnd::class.java)

        val waiting = AtomicInteger(0)

        private  fun addWaiter() {
            //need coroutine-aware synchronization method here
            log.info("Added a waiter to the existing $waiting waiters!")
            waiting.incrementAndGet()
        }

        /**
         * Let all waiting warmup ends continue.
         */
         fun startTest() {
            log.info("START TEST!")
            Event.signal(eventName)
            waiting.set(0)
        }

    }
}
