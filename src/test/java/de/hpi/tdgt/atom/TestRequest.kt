package de.hpi.tdgt.atom

import de.hpi.tdgt.HttpHandlers
import de.hpi.tdgt.RequestHandlingFramework
import de.hpi.tdgt.Utils
import de.hpi.tdgt.deserialisation.Deserializer.deserialize
import de.hpi.tdgt.test.story.atom.Atom
import de.hpi.tdgt.test.story.atom.Request
import de.hpi.tdgt.test.story.atom.Request.BasicAuth
import de.hpi.tdgt.test.story.atom.assertion.AssertionStorage
import de.hpi.tdgt.test.story.atom.assertion.ContentType
import de.hpi.tdgt.test.story.atom.assertion.ResponseCode
import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.IOException
import java.util.*
import java.util.concurrent.ExecutionException

class TestRequest : RequestHandlingFramework() {
    private var requestAtom: Request? = null
    private var postWithBodyAndAssertion: Request? = null
    private var getJsonObjectWithAssertion: Request? = null
    private var getWithAuth: Request? = null
    @BeforeEach
    @Throws(IOException::class)
    fun prepareTest() {
        requestAtom = Request()
        requestAtom!!.verb = "GET"
        requestAtom!!.addr = "http://example.com"
        requestAtom!!.assertions = arrayOf(ResponseCode())
        postWithBodyAndAssertion =
            deserialize(Utils().requestExampleWithAssertionsJSON).getStories()[0].getAtoms()[1] as Request
        getJsonObjectWithAssertion =
            deserialize(Utils().requestExampleWithAssertionsJSON).getStories()[0].getAtoms()[2] as Request
        getWithAuth =
            deserialize(Utils().requestExampleWithAssertionsJSON).getStories()[0].getAtoms()[3] as Request
    }

    @AfterEach
    fun clearAssertions() {
        AssertionStorage.instance.reset()
    }

    @Test
    fun cloneCreatesEquivalentObject() {
        val clone = requestAtom!!.clone()
        MatcherAssert.assertThat(clone, Matchers.equalTo<Atom?>(requestAtom))
    }

    @Test
    fun cloneCreatesEquivalentObjectWhenAllAttribvutesAreSet() {
        requestAtom!!.responseJSONObject = arrayOf("item1", "item2")
        //noch 10
        requestAtom!!.responseParams = arrayOf("item3", "item4")
        requestAtom!!.requestJSONObject = "{\"item5\":\$item5, \"item6\":\$item6}"
        requestAtom!!.requestParams = arrayOf("item7", "item8")
        requestAtom!!.basicAuth = BasicAuth("user", "pw")
        requestAtom!!.id = 0
        requestAtom!!.name = "Some Request"
        requestAtom!!.predecessorCount = 1
        requestAtom!!.repeat = 3
        requestAtom!!.setSuccessors(IntArray(0))
        val clone = requestAtom!!.clone()
        //would have been set by story
        clone.predecessorCount = 1
        MatcherAssert.assertThat(clone, Matchers.equalTo<Atom?>(requestAtom))
    }

    @Test
    fun cloneCreatesotherObject() {
        val clone = requestAtom!!.clone()
        Assertions.assertNotSame(clone, requestAtom)
    }

    @Test
    @Throws(InterruptedException::class, ExecutionException::class)
    fun ContentTypeAssertNotFailingIfCorrect() {
        val params = HashMap<String, String>()
        params["key"] = "something"
        params["value"] = "somethingElse"
        postWithBodyAndAssertion!!.run(params)
        MatcherAssert.assertThat(
            AssertionStorage.instance.getFails("postWithBody returns JSON"),
            Matchers.`is`(0)
        )
    }

    @Test
    @Throws(InterruptedException::class, ExecutionException::class)
    fun ContentTypeAssertFailingIfFalse() {
        val params = HashMap<String, String>()
        params["key"] = "something"
        params["value"] = "somethingElse"
        val assertion =
            postWithBodyAndAssertion!!.assertions[0] as ContentType
        //simulate failure
        assertion.contentType = "application/xml"
        postWithBodyAndAssertion!!.run(params)
        MatcherAssert.assertThat(
            AssertionStorage.instance.getFails("postWithBody returns JSON"),
            Matchers.`is`(1)
        )
    }

    @Test
    @Throws(InterruptedException::class, ExecutionException::class)
    fun ContentTypeAssertHasCorrectContentType() {
        val params = HashMap<String, String>()
        params["key"] = "something"
        params["value"] = "somethingElse"
        val assertion =
            postWithBodyAndAssertion!!.assertions[0] as ContentType
        //simulate failure
        assertion.contentType = "application/xml"
        postWithBodyAndAssertion!!.run(params)
        MatcherAssert.assertThat(
            AssertionStorage.instance.getActual("postWithBody returns JSON"),
            Matchers.contains("application/json")
        )
    }

    @Test
    @Throws(InterruptedException::class, ExecutionException::class)
    fun ContentNotEmptyAssertNotFailingIfCorrect() {
        val params = HashMap<String, String>()
        getJsonObjectWithAssertion!!.run(params)
        MatcherAssert.assertThat(
            AssertionStorage.instance.getFails("jsonObject returns something"),
            Matchers.`is`(0)
        )
    }

    @Test
    @Throws(InterruptedException::class, ExecutionException::class)
    fun ContentNotEmptyAssertFailingIfFalse() {
        val params = HashMap<String, String>()
        //simulate failure
        getJsonObjectWithAssertion!!.addr = "http://localhost:9000/empty"
        getJsonObjectWithAssertion!!.run(params)
        MatcherAssert.assertThat(
            AssertionStorage.instance.getFails("jsonObject returns something"),
            Matchers.`is`(1)
        )
    }

    @Test
    @Throws(InterruptedException::class, ExecutionException::class)
    fun ContentNotEmptyAssertHasCorrectContent() {
        val params = HashMap<String, String>()
        //simulate failure
        getJsonObjectWithAssertion!!.addr = "http://localhost:9000/empty"
        getJsonObjectWithAssertion!!.run(params)
        MatcherAssert.assertThat(
            AssertionStorage.instance.getActual("jsonObject returns something"),
            Matchers.contains("")
        )
    }

    @Test
    @Throws(InterruptedException::class, ExecutionException::class)
    fun ResponseAssertNotFailingIfCorrect() {
        val params = HashMap<String, String>()
        params["key"] = HttpHandlers.AuthHandler.username
        params["value"] = HttpHandlers.AuthHandler.password
        getWithAuth!!.run(params)
        MatcherAssert.assertThat(
            AssertionStorage.instance.getFails("auth does not return 401"),
            Matchers.`is`(0)
        )
    }

    @Test
    @Throws(InterruptedException::class, ExecutionException::class)
    fun ResponseAssertFailingIfFalse() {
        val params = HashMap<String, String>()
        params["key"] = "wrong"
        params["value"] = "wrong"
        getWithAuth!!.run(params)
        MatcherAssert.assertThat(
            AssertionStorage.instance.getFails("auth does not return 401"),
            Matchers.`is`(1)
        )
    }

    @Test
    @Throws(InterruptedException::class, ExecutionException::class)
    fun ResponseAssertHasCorrectResponseCode() {
        val params = HashMap<String, String>()
        params["key"] = "wrong"
        params["value"] = "wrong"
        getWithAuth!!.run(params)
        MatcherAssert.assertThat(
            AssertionStorage.instance.getActual("auth does not return 401"),
            Matchers.contains("401")
        )
    }

    @Test
    @Throws(InterruptedException::class, ExecutionException::class)
    fun ResponseAssertHasCorrectResponseCodeForDelete() {
        val params = HashMap<String, String>()
        params["key"] = "wrong"
        params["value"] = "wrong"
        getWithAuth!!.verb = "DELETE"
        getWithAuth!!.run(params)
        MatcherAssert.assertThat(
            AssertionStorage.instance.getActual("auth does not return 401"),
            Matchers.contains("401")
        )
    }

    @Test
    @Throws(InterruptedException::class, ExecutionException::class)
    fun resetOfAssertWorks() {
        val params = HashMap<String, String>()
        params["key"] = "wrong"
        params["value"] = "wrong"
        getWithAuth!!.run(params)
        AssertionStorage.instance.reset()
        MatcherAssert.assertThat(
            AssertionStorage.instance.getActual("auth does not return 401"),
            Matchers.empty()
        )
    }
}