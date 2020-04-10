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