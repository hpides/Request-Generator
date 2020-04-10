package de.hpi.tdgt.concurrency

import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.yield

/*
somewhat mimics the https://docs.microsoft.com/en-us/windows/win32/sync/event-objects synchronisation primitive.
But has global signaled state, for easy usage for system-wide events.
 */
object Event {

    /**
     * Return when event of this name has been signaled.
     */
    suspend fun waitFor(name: String) {
        while(true) {
            try {
                yield()
                lock.acquire()
                if (signaledEvents.contains(name)) {
                    return
                }
            } finally {
                lock.release()
            }
        }
    }

    /**
     * Release all waiters for the specified signal.
     */
    suspend fun signal(name: String) {
        for(i in 1..tickets){
            lock.acquire()
        }
        signaledEvents.add(name)
        for(i in 1..tickets){
            lock.release()
        }
    }

    /**
     * Release all waiters for the specified signal.
     */
    suspend fun unsignal(name: String) {
        for(i in 1..tickets){
            lock.acquire()
        }
        signaledEvents.remove(name)
        for(i in 1..tickets){
            lock.release()
        }
    }

    private val signaledEvents = HashSet<String>()

    private const val tickets = 1000
    //use 1000 tickets to mimic reader-writer-locks (readers take one ticket, writers all 1000). Unfortunately, there is no non-blocking implementation of this...
    private val lock = Semaphore(tickets,0)

    suspend fun reset(){
        for(i in 1..tickets){
            lock.acquire()
        }
        signaledEvents.clear()
        for(i in 1..tickets){
            lock.release()
        }
    }
}