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

package de.hpi.tdgt.deserialisation

import de.hpi.tdgt.Utils
import de.hpi.tdgt.deserialisation.Deserializer.deserialize
import de.hpi.tdgt.test.Test
import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import java.io.IOException

class DeserializeStoryTest {
    @get:Throws(IOException::class)
    private val exampleJSON: String
        private get() = Utils().exampleJSON

    private var deserializedTest: Test? = null
    @BeforeEach
    @Throws(IOException::class)
    fun prepareTest() {
        deserializedTest = deserialize(exampleJSON)
    }

    @org.junit.jupiter.api.Test
    @Throws(IOException::class)
    fun hasTwoStories() {
        Assertions.assertEquals(deserializedTest!!.getStories().size, 2)
    }

    @org.junit.jupiter.api.Test
    @Throws(IOException::class)
    fun firstStoryHasEightAtoms() {
        Assertions.assertEquals(deserializedTest!!.getStories()[0].getAtoms().size, 9)
    }

    @org.junit.jupiter.api.Test
    @Throws(IOException::class)
    fun secondStoryHasFiveAtoms() {
        Assertions.assertEquals(deserializedTest!!.getStories()[1].getAtoms().size, 5)
    }

    @org.junit.jupiter.api.Test
    fun cloneCreatesEquivalentStory() {
        val story = deserializedTest!!.getStories()[0]
        val clone = story.clone()
        val firstAtom = clone.getAtoms()[0]
        MatcherAssert.assertThat(
            firstAtom.successorLinks[0].successorLinks[0].name,
            Matchers.equalTo("User anlegen")
        )
    }

    @org.junit.jupiter.api.Test
    fun cloneCreatesNewObject() {
        val story = deserializedTest!!.getStories()[0]
        val firstAtom = story.getAtoms()[1]
        val clone = firstAtom.clone()
        Assertions.assertNotSame(clone, firstAtom)
    }

    @org.junit.jupiter.api.Test
    fun cloneCreatesEqualObject() {
        val story = deserializedTest!!.getStories()[0]
        val clone = story.clone()
        MatcherAssert.assertThat(
            clone.getAtoms()[0],
            Matchers.equalTo(story.getAtoms()[0])
        )
    }

    @org.junit.jupiter.api.Test
    fun cloneCreatesNewSuccessorObjects() {
        val story = deserializedTest!!.getStories()[0]
        val clone = story.clone()
        Assertions.assertNotSame(clone.getAtoms()[1], story.getAtoms()[1])
    }
}