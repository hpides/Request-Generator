package de.hpi.tdgt.atom

import de.hpi.tdgt.test.story.atom.DelayAtom
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.MatcherAssert
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class TestDelayAtom {
    private var delayAtomAtom: DelayAtom? = null

    @BeforeEach
    fun prepareTest() {
        delayAtomAtom = DelayAtom()
    }

    @Test
    fun delay100ms(){
        delayAtomAtom!!.delayMs = 100
        val startTime = System.currentTimeMillis()
        runBlocking {
            delayAtomAtom!!.perform()
        }
        val endTime = System.currentTimeMillis()
        Assertions.assertTrue(endTime - startTime > 90)
    }

    @Test
    fun delay1000ms() {
        delayAtomAtom!!.delayMs = 1000
        val startTime = System.currentTimeMillis()
        //else we will not wait at all
        runBlocking {
            delayAtomAtom!!.perform()
        }
        val endTime = System.currentTimeMillis()
        Assertions.assertTrue(endTime - startTime > 990)
    }

    @Test
    fun cloneCreatesEquivalentObject() = runBlockingTest {
        val clone = delayAtomAtom!!.clone()
        MatcherAssert.assertThat("Clone should equal the cloned object!",clone.equals(delayAtomAtom))
    }

    @Test
    fun cloneCreatesOtherObject() = runBlockingTest {
        val clone = delayAtomAtom!!.clone()
        Assertions.assertNotSame(clone, delayAtomAtom)
    }
}