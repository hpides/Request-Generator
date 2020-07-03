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

package de.hpi.tdgt.test.story.atom

import de.hpi.tdgt.concurrency.Event
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.withContext
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

/**
 * This atom signals the end of the warmup phase.
 * There can be multiple in a test suite, but they have to be in parallel branches.
 */
class WarmupEnd : Atom() {
    @Throws(InterruptedException::class)
    override suspend fun perform() {
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
