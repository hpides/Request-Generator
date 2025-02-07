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

import de.hpi.tdgt.HttpHandlers.PostBodyHandler
import de.hpi.tdgt.RequestHandlingFramework
import de.hpi.tdgt.Utils
import de.hpi.tdgt.deserialisation.Deserializer.deserialize
import de.hpi.tdgt.test.Test
import kotlinx.coroutines.runBlocking
import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
import org.junit.jupiter.api.BeforeEach
import java.io.IOException
import java.util.concurrent.ExecutionException
import java.util.concurrent.Future

class TestWarmup : RequestHandlingFramework() {
    private var warmupTest: Test? = null
    @BeforeEach
    @Throws(IOException::class)
    fun prepare() {
        warmupTest =
            deserialize(Utils().requestExampleWithAssertionsAndWarmupJSON)
    }

    @org.junit.jupiter.api.Test
    @Throws(InterruptedException::class, ExecutionException::class)
    fun testWarmupCallsPreparationActivities() {
        runBlocking {
            val threads = warmupTest!!.warmup()
            MatcherAssert.assertThat(postBodyHandler.requests_total, Matchers.`is`(7))
            warmupTest!!.start(threads)
        }
    }

    @org.junit.jupiter.api.Test
    @Throws(InterruptedException::class, ExecutionException::class)
    fun testWarmupCallsNoOtherActivities() {
        runBlocking {
            val threads = warmupTest!!.warmup()
            for (handler in handlers) {
                if (handler !is PostBodyHandler) {
                    MatcherAssert.assertThat(handler.requests_total, Matchers.`is`(0))
                }
            }
            warmupTest!!.start(threads)
        }
    }

    @org.junit.jupiter.api.Test
    @Throws(InterruptedException::class, ExecutionException::class)
    fun testStoriesAreCompletedAfterWarmup() {
        runBlocking {
            val threads = warmupTest!!.warmup()
            warmupTest!!.start(threads)
        }
        //7 in first story, 30 in second story
        MatcherAssert.assertThat(authHandler.requests_total, Matchers.`is`(37))
    }

    @org.junit.jupiter.api.Test
    @Throws(InterruptedException::class, ExecutionException::class)
    fun testStoriesAreCompletedAfterWarmupWithRepeat() {
        warmupTest!!.repeat = 3
        runBlocking {
            val threads = warmupTest!!.warmup()
            warmupTest!!.start(threads)
        }
        //7 in first story, 30 in second story
        MatcherAssert.assertThat(authHandler.requests_total, Matchers.`is`(3 * 37))
    }

    @org.junit.jupiter.api.Test
    fun testFirstStoryHasWarmup() {
        MatcherAssert.assertThat(
            warmupTest!!.getStories()[0].hasWarmup(),
            Matchers.`is`(true)
        )
    }

    @org.junit.jupiter.api.Test
    fun testSecondStoryHasNoWarmup() {
        MatcherAssert.assertThat(
            warmupTest!!.getStories()[1].hasWarmup(),
            Matchers.`is`(false)
        )
    }

    @org.junit.jupiter.api.Test
    fun testSecondStoryHasNoWarmupInNumbers() {
        MatcherAssert.assertThat(
            warmupTest!!.getStories()[1].numberOfWarmupEnds(),
            Matchers.`is`(0)
        )
    }

    @org.junit.jupiter.api.Test
    fun testFirstStoryHasOneWarmupInNumbers() {
        MatcherAssert.assertThat(
            warmupTest!!.getStories()[0].numberOfWarmupEnds(),
            Matchers.`is`(1)
        )
    }
}