package de.hpi.tdgt.atom

import de.hpi.tdgt.test.story.atom.Delay
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
        delayAtom!!.repeat = 1
        delayAtom!!.predecessorCount = 0
    }

    @Test
    fun delay100ms(){
        delayAtom!!.delayMs = "100"
        val startTime = System.currentTimeMillis()
            delayAtom!!.perform()
        
        val endTime = System.currentTimeMillis()
        Assertions.assertTrue(endTime - startTime > 90)
    }

    @Test
    fun delay1000ms() {
        delayAtom!!.delayMs = "1000"
        val startTime = System.currentTimeMillis()
        //else we will not wait at all
            delayAtom!!.perform()
        
        val endTime = System.currentTimeMillis()
        Assertions.assertTrue(endTime - startTime > 990)
    }

    @Test
    fun delayInterpretsString() {
        delayAtom!!.delayMs = "1\$delay0"
        val startTime = System.currentTimeMillis()
        val params = HashMap<String, String>()
        params["delay"] = "00"
        //else we will not wait at all
            delayAtom!!.run(params)

        val endTime = System.currentTimeMillis()
        Assertions.assertTrue(endTime - startTime > 990)
    }

    @Test
    fun cloneCreatesEquivalentObject() = run {
        val clone = delayAtom!!.clone()
        MatcherAssert.assertThat("Clone should equal the cloned object!",clone.equals(delayAtom))
    }

    @Test
    fun cloneCreatesOtherObject() = run {
        val clone = delayAtom!!.clone()
        Assertions.assertNotSame(clone, delayAtom)
    }
}