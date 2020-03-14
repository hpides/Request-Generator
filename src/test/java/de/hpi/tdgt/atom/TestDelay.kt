package de.hpi.tdgt.atom

import de.hpi.tdgt.test.story.atom.Delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class TestDelay {
    private var delayAtom: Delay? = null

    @BeforeEach
    fun prepareTest() {
        delayAtom = Delay()
    }

    @Test
    fun delay100ms(){
        delayAtom!!.delayMs = 100
        val startTime = System.currentTimeMillis()
        runBlocking {
            delayAtom!!.perform()
        }
        val endTime = System.currentTimeMillis()
        Assertions.assertTrue(endTime - startTime > 90)
    }

    @Test
    fun delay1000ms() {
        delayAtom!!.delayMs = 1000
        val startTime = System.currentTimeMillis()
        //else we will not wait at all
        runBlocking {
            delayAtom!!.perform()
        }
        val endTime = System.currentTimeMillis()
        Assertions.assertTrue(endTime - startTime > 990)
    }

    @Test
    fun cloneCreatesEquivalentObject() = runBlockingTest {
        val clone = delayAtom!!.clone()
        MatcherAssert.assertThat("Clone should equal the cloned object!",clone.equals(delayAtom))
    }

    @Test
    fun cloneCreatesOtherObject() = runBlockingTest {
        val clone = delayAtom!!.clone()
        Assertions.assertNotSame(clone, delayAtom)
    }
}