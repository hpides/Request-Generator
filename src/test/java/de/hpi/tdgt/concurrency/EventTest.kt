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

import kotlinx.coroutines.*
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.lang.Thread.sleep
import java.util.concurrent.atomic.AtomicInteger

class EventTest {
    @BeforeEach
    fun reset() = runBlocking{
        Event.reset()
    }

    var ran = false

    @Test
    public fun coroutinesDoNotTerminateUntilEventIsSignaled() {
        GlobalScope.launch {
            Event.waitFor("testEvent")
            ran = true
        }
        sleep(1000)
        assertThat("Coroutine should not have run",!ran);
    }

    @Test
    public fun coroutinesTerminateAfterEventIsSignaled() {
        GlobalScope.launch {
            Event.waitFor("testEvent")
            ran = true
        }
        GlobalScope.launch {Event.signal("testEvent")}
        sleep(1000)
        assertThat("Coroutine should have run",ran);
    }

    @Test
    public fun coroutinesTerminateAfterEventIsSignaledForManyCoroutines() {
        val ran = AtomicInteger(0)
        for(i in 1..1000) {
            GlobalScope.launch {
                Event.waitFor("testEvent")
                ran.incrementAndGet()
            }
        }
        GlobalScope.launch {Event.signal("testEvent")}
        sleep(1000)
        assertThat("All 1000 Coroutines should have run",ran.get() == 1000);
    }
}