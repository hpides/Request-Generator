package de.hpi.tdgt.concurrency

import java.lang.Thread.sleep
import java.lang.Thread.yield
import java.util.concurrent.locks.ReentrantReadWriteLock
import java.util.concurrent.locks.StampedLock
import kotlin.concurrent.read
import kotlin.concurrent.write


/*
somewhat mimics the https://docs.microsoft.com/en-us/windows/win32/sync/event-objects synchronisation primitive.
But has global signaled state, for easy usage for system-wide events.
 */
object Event {

    /**
     * Return when event of this name has been signaled.
     */
     fun waitFor(name: String) {
        while(true) {
                sleep(0,1000)
                lock.read {
                    if (signaledEvents.contains(name)) {
                        return
                    }
                }
        }
    }

    /**
     * Release all waiters for the specified signal.
     */
     fun signal(name: String) {

        lock.write { signaledEvents.add(name) }


    }

    /**
     * Release all waiters for the specified signal.
     */
     fun unsignal(name: String) {
        lock.write { signaledEvents.remove(name) }

    }

    private val signaledEvents = HashSet<String>()

    private val lock = ReentrantReadWriteLock()

     fun reset(){
        lock.write { signaledEvents.clear() }
    }
}