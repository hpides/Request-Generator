package de.hpi.tdgt.concurrency

import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.yield
import java.util.concurrent.locks.ReentrantReadWriteLock
object Event {
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
    suspend fun signal(name: String) {
        for(i in 1..tickets){
            lock.acquire()
        }
        signaledEvents.add(name)
        for(i in 1..tickets){
            lock.release()
        }
    }

    private val signaledEvents = HashSet<String>()

    private val tickets = 1000

    private val lock = Semaphore(tickets,0)

    public suspend fun reset(){
        for(i in 1..tickets){
            lock.acquire()
        }
        signaledEvents.clear()
        for(i in 1..tickets){
            lock.release()
        }
    }
}