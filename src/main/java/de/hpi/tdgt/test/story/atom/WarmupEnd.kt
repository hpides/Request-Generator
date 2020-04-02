package de.hpi.tdgt.test.story.atom

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.withContext
import org.apache.logging.log4j.LogManager

/**
 * This atom signals the end of the warmup phase.
 * There can be multiple in a test suite, but they have to be in parallel branches.
 */
class WarmupEnd : Atom() {
    @Throws(InterruptedException::class)
    override suspend fun perform() {
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
        private val warmupEnd = Semaphore(1)
        var waiting = 0
            private set

        var toInitialize = true

        private val mutex = Semaphore(1)

        private suspend fun addWaiter() {
            //need coroutine-aware synchronization method here
            mutex.acquire()
            //Semaphores can not be created without permits, so build a dummy coroutine that aquires the additional permit
            if (toInitialize) {
                withContext(Dispatchers.Default) {
                    async { warmupEnd.acquire() }
                }
                toInitialize = false
            }
            log.info("Added a waiter to the existing $waiting waiters!")
            waiting++
            mutex.release()
        }

        /**
         * Let all waiting warmup ends continue.
         */
        fun startTest() {
            log.info("START TEST!");
            warmupEnd.release(waiting)
            waiting = 0
        }

    }
}

private fun Semaphore.release(waiting: Int) {
    for(i in 0 until waiting){
        release()
    }
}
