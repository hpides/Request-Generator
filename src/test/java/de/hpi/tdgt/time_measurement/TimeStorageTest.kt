package de.hpi.tdgt.time_measurement

import de.hpi.tdgt.RequestHandlingFramework
import de.hpi.tdgt.Utils
import de.hpi.tdgt.deserialisation.Deserializer.deserialize
import de.hpi.tdgt.test.story.atom.Request
import de.hpi.tdgt.test.time_measurement.TimeStorage
import org.apache.logging.log4j.LogManager
import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
import org.junit.jupiter.api.Test
import java.io.IOException
import java.util.concurrent.ExecutionException

class TimeStorageTest : RequestHandlingFramework() {
    @Test
    @Throws(IOException::class, InterruptedException::class, ExecutionException::class)
    fun testFirstRequestOfFirstStoryTakesTime() {
        val test =
            deserialize(Utils().requestExampleJSON)
        //do not run second story for this time around; messes with results
        test.setStories(arrayOf(test.getStories()[0]))
        test.start()
        val storage = TimeStorage.instance
        val firstRequest =
            test.getStories()[0].getAtoms()[2] as Request
        MatcherAssert.assertThat(
            storage.getTimes(firstRequest.verb, firstRequest.addr).size,
            Matchers.greaterThan(0)
        )
    }

    @Test
    @Throws(IOException::class, InterruptedException::class, ExecutionException::class)
    fun testFirstRequestOfFirstStoryHasMaxTimeOverNull() {
        val test =
            deserialize(Utils().requestExampleJSON)
        //do not run second story for this time around; messes with results
        test.setStories(arrayOf(test.getStories()[0]))
        test.start()
        val storage = TimeStorage.instance
        val firstRequest =
            test.getStories()[0].getAtoms()[2] as Request
        MatcherAssert.assertThat(
            storage.getMax(firstRequest.verb, firstRequest.addr),
            Matchers.greaterThan(0L)
        )
    }

    @Test
    @Throws(IOException::class, InterruptedException::class, ExecutionException::class)
    fun testFirstRequestOfFirstStoryHasMinTimeOverNull() {
        val test =
            deserialize(Utils().requestExampleJSON)
        //do not run second story for this time around; messes with results
        test.setStories(arrayOf(test.getStories()[0]))
        test.start()
        val storage = TimeStorage.instance
        val firstRequest =
            test.getStories()[0].getAtoms()[2] as Request
        MatcherAssert.assertThat(
            storage.getMin(firstRequest.verb, firstRequest.addr),
            Matchers.greaterThan(0L)
        )
    }

    @Test
    @Throws(IOException::class, InterruptedException::class, ExecutionException::class)
    fun testFirstRequestOfFirstStoryHasAvgTimeOverNull() {
        val test =
            deserialize(Utils().requestExampleJSON)
        //do not run second story for this time around; messes with results
        test.setStories(arrayOf(test.getStories()[0]))
        test.start()
        val storage = TimeStorage.instance
        val firstRequest =
            test.getStories()[0].getAtoms()[2] as Request
        MatcherAssert.assertThat(
            storage.getAvg(firstRequest.verb, firstRequest.addr),
            Matchers.greaterThan(0.0)
        )
    }

    companion object {
        private val log = LogManager.getLogger(
            TimeStorageTest::class.java
        )
    }
}