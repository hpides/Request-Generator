package de.hpi.tdgt.deserialisation

import de.hpi.tdgt.Utils
import de.hpi.tdgt.deserialisation.Deserializer.deserialize
import de.hpi.tdgt.requesthandling.HttpConstants
import de.hpi.tdgt.test.story.atom.*
import de.hpi.tdgt.test.story.atom.assertion.Assertion
import de.hpi.tdgt.test.story.atom.assertion.ContentNotEmpty
import de.hpi.tdgt.test.story.atom.assertion.ContentType
import de.hpi.tdgt.test.story.atom.assertion.ResponseCode
import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.IOException
import java.util.*

class DeserializeAtomTest {
    @get:Throws(IOException::class)
    private val exampleJSON: String
        private get() = Utils().exampleJSON

    private var firstAtomOfFirstStory: Atom? = null
    private var secondAtomOfFirstStory: Atom? = null
    private var thirdAtomOfFirstStory: Atom? = null
    private var seventhAtomOfFirstStory: Atom? = null
    private var secondAtomOfSecondStory: Atom? = null
    private var postWithBodyAndAssertion: Request? = null
    private var getJSONObject: Request? = null
    private var getWithAuth: Request? = null
    @BeforeEach
    @Throws(IOException::class)
    fun prepareTest() {
        firstAtomOfFirstStory =
            deserialize(exampleJSON).getStories()[0].getAtoms()[0]
        secondAtomOfFirstStory =
            deserialize(exampleJSON).getStories()[0].getAtoms()[1]
        thirdAtomOfFirstStory =
            deserialize(exampleJSON).getStories()[0].getAtoms()[2]
        seventhAtomOfFirstStory =
            deserialize(exampleJSON).getStories()[0].getAtoms()[6]
        secondAtomOfSecondStory =
            deserialize(exampleJSON).getStories()[1].getAtoms()[3]
        postWithBodyAndAssertion =
            deserialize(Utils().requestExampleWithAssertionsJSON).getStories()[0].getAtoms()[1] as Request
        getJSONObject =
            deserialize(Utils().requestExampleWithAssertionsJSON).getStories()[0].getAtoms()[2] as Request
        getWithAuth =
            deserialize(Utils().requestExampleWithAssertionsJSON).getStories()[0].getAtoms()[3] as Request
    }

    @Test
    fun firstAtomOfFirstStoryIsDataGeneration() {
        Utils.assertInstanceOf(secondAtomOfFirstStory, Data_Generation::class.java)
    }

    @Test
    fun sixthAtomOfFirstStoryIsDelay() {
        Utils.assertInstanceOf(seventhAtomOfFirstStory, Delay::class.java)
    }

    @Test
    fun secondAtomOfSecondStoryIsRequest() {
        Utils.assertInstanceOf(secondAtomOfSecondStory, Request::class.java)
    }

    @Test
    @Throws(IOException::class)
    fun firstAtomOfFirstStoryGetsUsernameAndPasswordFromUsers() {
        val firstAtomOfFirstStory = secondAtomOfFirstStory as Data_Generation?
        Assertions.assertArrayEquals(
            arrayOf("username", "password"),
            firstAtomOfFirstStory!!.data
        )
        Assertions.assertEquals("users", firstAtomOfFirstStory.table)
    }

    @Test
    fun sixthAtomOfFirstStoryWaitsOneSecond() {
        val sixthAtomOfFirstStory =
            seventhAtomOfFirstStory as Delay?
        Assertions.assertEquals(1000, sixthAtomOfFirstStory!!.delayMs)
    }

    @Test
    fun secondAtomOfSecondStorySendsGETRequest() {
        val secondAtomOfSecondStory =
            secondAtomOfSecondStory as Request?
        Assertions.assertEquals("GET", secondAtomOfSecondStory!!.verb)
    }

    @Test
    fun secondAtomOfSecondStoryHasCorrectAddress() {
        val secondAtomOfSecondStory =
            secondAtomOfSecondStory as Request?
        //I sometimes disable this endpoint to test assertions by appending "/not"
        MatcherAssert.assertThat(
            secondAtomOfSecondStory!!.addr,
            Matchers.startsWith("http://search/posts/search")
        )
    }

    @Test
    fun secondAtomOfSecondStoryHasCorrectRequestParams() {
        val secondAtomOfSecondStory =
            secondAtomOfSecondStory as Request?
        Assertions.assertArrayEquals(
            arrayOf("key"),
            secondAtomOfSecondStory!!.requestParams
        )
    }

    @Test
    fun secondAtomOfSecondStoryHasCorrectResponseParams() {
        val secondAtomOfSecondStory =
            secondAtomOfSecondStory as Request?
        Assertions.assertArrayEquals(null, secondAtomOfSecondStory!!.responseParams)
    }

    @Test
    fun lastAtomOfSecondStoryHasNoSuccessors() {
        Assertions.assertArrayEquals(
            arrayOfNulls<Atom>(0),
            secondAtomOfSecondStory!!.successorLinks[0].successorLinks
        )
    }

    @Test
    fun firstAtomOfFirstStoryHasOneSuccessor() {
        Assertions.assertEquals(1, secondAtomOfFirstStory!!.successorLinks.size)
    }

    @Test
    fun firstAtomOfFirstStoryHasCorrectSuccessor() {
        Assertions.assertEquals(2, secondAtomOfFirstStory!!.successorLinks[0].id)
    }

    @Test
    fun secondAtomOfFirstStoryHasCorrectSuccessors() {
        val successors = Vector<Int>()
        //no implicit parallelism here, we need the exact order for the assert
        for (successor in thirdAtomOfFirstStory!!.successorLinks) {
            successors.add(successor.id)
        }
        Assertions.assertArrayEquals(arrayOf(3, 5), successors.toTypedArray())
    }

    @Test
    fun postWithBodyAndAssertionAssertsContentType() {
        MatcherAssert.assertThat<Assertion>(
            postWithBodyAndAssertion!!.assertions[0], Matchers.instanceOf(
                ContentType::class.java
            )
        )
    }

    @Test
    fun postWithBodyAndAssertionAssertsContentTypeJSON() {
        val assertion =
            postWithBodyAndAssertion!!.assertions[0] as ContentType
        MatcherAssert.assertThat(
            assertion.contentType,
            Matchers.equalTo(HttpConstants.CONTENT_TYPE_APPLICATION_JSON)
        )
    }

    @get:Test
    val jsonObjectWithAssertionAssertsContentNotEmpty: Unit
        get() {
            MatcherAssert.assertThat<Assertion>(
                getJSONObject!!.assertions[0], Matchers.instanceOf(
                    ContentNotEmpty::class.java
                )
            )
        }

    @get:Test
    val withAuthAndAssertionAssertsResponseCode: Unit
        get() {
            MatcherAssert.assertThat<Assertion>(
                getWithAuth!!.assertions[0], Matchers.instanceOf(
                    ResponseCode::class.java
                )
            )
        }

    @get:Test
    val withAuthAndAssertionAssertsResponseCode200: Unit
        get() {
            val code = getWithAuth!!.assertions[0] as ResponseCode
            MatcherAssert.assertThat(code.responseCode, Matchers.`is`(200))
        }

    @Test
    fun setFirstAtomOfFirstStoryIsStart() {
        MatcherAssert.assertThat(
            firstAtomOfFirstStory,
            Matchers.instanceOf(Start::class.java)
        )
    }
}