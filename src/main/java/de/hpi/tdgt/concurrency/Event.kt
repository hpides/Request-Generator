/*
 * WALT - A realistic load generator for web applications.
 *
 * Copyright 2020 Eric Ackermann <eric.ackermann@student.hpi.de>, Hendrik Bomhardt
 * <hendrik.bomhardt@student.hpi.de>, Benito Buchheim
 * <benito.buchheim@student.hpi.de>, Juergen Schlossbauer
 * <juergen.schlossbauer@student.hpi.de>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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