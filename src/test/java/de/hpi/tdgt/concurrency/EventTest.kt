package de.hpi.tdgt.concurrency

import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.lang.Thread.sleep
import java.util.concurrent.atomic.AtomicInteger

class EventTest {
    @BeforeEach
    fun reset() = run{
        Event.reset()
    }

    var ran = false

    @Test
    public fun coroutinesDoNotTerminateUntilEventIsSignaled() {
        Thread.startVirtualThread {
            Event.waitFor("testEvent")
            ran = true
        }
        sleep(1000)
        assertThat("Coroutine should not have run",!ran);
    }

    @Test
    public fun coroutinesTerminateAfterEventIsSignaled() {
        Thread.startVirtualThread {
            Event.waitFor("testEvent")
            ran = true
        }
        Thread.startVirtualThread {Event.signal("testEvent")}
        sleep(1000)
        assertThat("Coroutine should have run",ran);
    }

    @Test
    public fun coroutinesTerminateAfterEventIsSignaledForManyCoroutines() {
        val ran = AtomicInteger(0)
        for(i in 1..1000) {
            Thread.startVirtualThread {
                Event.waitFor("testEvent")
                ran.incrementAndGet()
            }
        }
        Thread.startVirtualThread {Event.signal("testEvent")}
        sleep(1000)
        assertThat("All 1000 Coroutines should have run",ran.get() == 1000);
    }
}