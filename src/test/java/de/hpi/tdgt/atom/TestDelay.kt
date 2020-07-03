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
        delayAtom!!.repeat = 1
        delayAtom!!.predecessorCount = 0
    }

    @Test
    fun delay100ms(){
        delayAtom!!.delayMs = "100"
        val startTime = System.currentTimeMillis()
        runBlocking {
            delayAtom!!.perform()
        }
        val endTime = System.currentTimeMillis()
        Assertions.assertTrue(endTime - startTime > 90)
    }

    @Test
    fun delay1000ms() {
        delayAtom!!.delayMs = "1000"
        val startTime = System.currentTimeMillis()
        //else we will not wait at all
        runBlocking {
            delayAtom!!.perform()
        }
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
        runBlocking {
            delayAtom!!.run(params)

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