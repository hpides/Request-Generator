package de.hpi.tdgt.atom

import de.hpi.tdgt.HttpHandlers
import de.hpi.tdgt.RequestHandlingFramework
import de.hpi.tdgt.Utils
import de.hpi.tdgt.controllers.UploadController
import de.hpi.tdgt.deserialisation.Deserializer.deserialize
import de.hpi.tdgt.requesthandling.RestClient
import de.hpi.tdgt.test.story.UserStory
import de.hpi.tdgt.test.story.atom.Atom
import de.hpi.tdgt.test.story.atom.Request
import de.hpi.tdgt.test.story.atom.Request.BasicAuth
import de.hpi.tdgt.test.story.atom.assertion.AssertionStorage
import de.hpi.tdgt.test.story.atom.assertion.ContentType
import de.hpi.tdgt.test.story.atom.assertion.ResponseCode
import kotlinx.coroutines.runBlocking
import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.IOException
import java.net.URL
import java.util.concurrent.ExecutionException
import kotlin.collections.HashMap

class TestRequest : RequestHandlingFramework() {
    private var requestAtom: Request? = null
    private var postWithBodyAndAssertion: Request? = null
    private var getJsonObjectWithAssertion: Request? = null
    private var getWithAuth: Request? = null
    private val mockLocation = "somewhere"
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
    UploadController.LOCATION = mockLocation
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
        runBlocking{postWithBodyAndAssertion!!.run(params)}
        MatcherAssert.assertThat(
            AssertionStorage.instance.getFails("postWithBody returns JSON"),
            Matchers.`is`(0)
        )
    }
    @Test
    @Throws(InterruptedException::class, ExecutionException::class)
    fun canSendPOSTWithForm() {
        val rc = Request()
        val params = java.util.HashMap<String, String>()
        params["param"] = "value"
        rc.addr="http://localhost:9000/echoPost"
        rc.verb="POST"
        rc.responseJSONObject=arrayOf("param")
        rc.requestParams= arrayOf("param")
        rc.predecessorCount = -1
        rc.repeat = 1
        runBlocking { rc.run(params) }

        MatcherAssert.assertThat("PostBodyHandler should have accepted this post!",
                postHandler.lastPostWasOkay,
                Matchers.`is`(true)
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
        runBlocking{postWithBodyAndAssertion!!.run(params)}
        MatcherAssert.assertThat(
            AssertionStorage.instance.getFails("postWithBody returns JSON (node "+ UploadController.LOCATION+")"),
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
        runBlocking{postWithBodyAndAssertion!!.run(params)}
        MatcherAssert.assertThat(
            AssertionStorage.instance.getActual("postWithBody returns JSON (node "+UploadController.LOCATION+")"),
            Matchers.contains("application/json")
        )
    }

    @Test
    @Throws(InterruptedException::class, ExecutionException::class)
    fun ContentNotEmptyAssertNotFailingIfCorrect() {
        val params = HashMap<String, String>()
        runBlocking{getJsonObjectWithAssertion!!.run(params)}
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
        runBlocking{getJsonObjectWithAssertion!!.run(params)}
        MatcherAssert.assertThat(
            AssertionStorage.instance.getFails("jsonObject returns something (node "+UploadController.LOCATION+")"),
            Matchers.`is`(1)
        )
    }

    @Test
    @Throws(InterruptedException::class, ExecutionException::class)
    fun ContentNotEmptyAssertHasCorrectContent() {
        val params = HashMap<String, String>()
        //simulate failure
        getJsonObjectWithAssertion!!.addr = "http://localhost:9000/empty"
        runBlocking{getJsonObjectWithAssertion!!.run(params)}
        MatcherAssert.assertThat(
            AssertionStorage.instance.getActual("jsonObject returns something (node "+UploadController.LOCATION+")"),
            Matchers.contains("")
        )
    }

    @Test
    @Throws(InterruptedException::class, ExecutionException::class)
    fun ResponseAssertNotFailingIfCorrect() {
        val params = HashMap<String, String>()
        params["key"] = HttpHandlers.AuthHandler.username
        params["value"] = HttpHandlers.AuthHandler.password
        runBlocking{getWithAuth!!.run(params)}
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
        runBlocking{getWithAuth!!.run(params)}
        MatcherAssert.assertThat(
            AssertionStorage.instance.getFails("auth does not return 401 (node "+UploadController.LOCATION+")"),
            Matchers.`is`(1)
        )
    }

    @Test
    @Throws(InterruptedException::class, ExecutionException::class)
    fun ResponseAssertHasCorrectResponseCode() {
        val params = HashMap<String, String>()
        params["key"] = "wrong"
        params["value"] = "wrong"
        runBlocking{getWithAuth!!.run(params)}
        MatcherAssert.assertThat(
            AssertionStorage.instance.getActual("auth does not return 401 (node "+UploadController.LOCATION+")"),
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
        runBlocking{getWithAuth!!.run(params)}
        MatcherAssert.assertThat(
            AssertionStorage.instance.getActual("auth does not return 401 (node "+UploadController.LOCATION+")"),
            Matchers.contains("401")
        )
    }

    @Test
    @Throws(InterruptedException::class, ExecutionException::class)
    fun resetOfAssertWorks() {
        val params = HashMap<String, String>()
        params["key"] = "wrong"
        params["value"] = "wrong"
        runBlocking{getWithAuth!!.run(params)}
        AssertionStorage.instance.reset()
        MatcherAssert.assertThat(
            AssertionStorage.instance.getActual("auth does not return 401"),
            Matchers.empty()
        )
    }
    @Test
    fun readsCookie(){
        val story = UserStory()
        story.name = "story"
        val test = de.hpi.tdgt.test.Test()
        story.parent = test
        requestAtom!!.verb = "GET"
        requestAtom!!.addr = "http://localhost:9000/cookie"
        val cookies = HashMap<String, String>()
        cookies.put("JSESSIONID","id");
        requestAtom!!.receiveCookies = cookies
        requestAtom!!.setSuccessors(IntArray(0))
        requestAtom!!.predecessorCount = 0
        requestAtom!!.repeat = 1
        requestAtom!!.setParent(story)
        runBlocking {requestAtom!!.run(HashMap())}
        MatcherAssert.assertThat(
            requestAtom!!.knownParams,
            Matchers.hasEntry(Matchers.equalTo("id"), Matchers.equalTo("1234567890"))
        )

    }

    @Test
    fun readsCookieByRegex(){
        val story = UserStory()
        story.name = "story"
        val test = de.hpi.tdgt.test.Test()
        story.parent = test
        requestAtom!!.verb = "GET"
        requestAtom!!.addr = "http://localhost:9000/cookie"
        val cookies = HashMap<String, String>()
        cookies.put("J\\w+D","id");
        requestAtom!!.receiveCookies = cookies
        requestAtom!!.setSuccessors(IntArray(0))
        requestAtom!!.predecessorCount = 0
        requestAtom!!.repeat = 1
        requestAtom!!.setParent(story)
        runBlocking {requestAtom!!.run(HashMap())}
        MatcherAssert.assertThat(
            requestAtom!!.knownParams,
            Matchers.hasEntry(Matchers.equalTo("id"), Matchers.equalTo("1234567890"))
        )

    }

    @Test
    fun readsMultipleCookies(){
        val story = UserStory()
        story.name = "story"
        val test = de.hpi.tdgt.test.Test()
        story.parent = test
        requestAtom!!.verb = "GET"
        requestAtom!!.addr = "http://localhost:9000/cookie?multiple=true"
        val cookies = HashMap<String, String>()
        cookies.put("JSESSIONID","id");
        cookies.put("SomethingElse","something");
        requestAtom!!.receiveCookies = cookies
        requestAtom!!.setSuccessors(IntArray(0))
        requestAtom!!.predecessorCount = 0
        requestAtom!!.repeat = 1
        requestAtom!!.setParent(story)
        runBlocking {requestAtom!!.run(HashMap())}
        MatcherAssert.assertThat(
            requestAtom!!.knownParams,
            Matchers.hasEntry(Matchers.equalTo("something"), Matchers.equalTo("abc"))
        )
    }

    @Test
    fun setsCookies(){
        val story = UserStory()
        story.name = "story"
        val test = de.hpi.tdgt.test.Test()
        story.parent = test
        requestAtom!!.verb = "GET"
        requestAtom!!.addr = "http://localhost:9000/cookie"
        val cookies = HashMap<String, String>()
        cookies.put("cookie","JSESSIONID");
        requestAtom!!.sendCookies = cookies
        requestAtom!!.setSuccessors(IntArray(0))
        requestAtom!!.predecessorCount = 0
        requestAtom!!.repeat = 1
        requestAtom!!.setParent(story)
        val params = HashMap<String,String>()
        params.put("cookie","1234")
        runBlocking {requestAtom!!.run(params)}
        MatcherAssert.assertThat(
            cookiehandler.lastCookie,
            Matchers.containsString("1234")
        )

    }

    @Test
    fun setsCookiesByRegex(){
        val story = UserStory()
        story.name = "story"
        val test = de.hpi.tdgt.test.Test()
        story.parent = test
        requestAtom!!.verb = "GET"
        requestAtom!!.addr = "http://localhost:9000/cookie"
        val cookies = HashMap<String, String>()
        cookies.put("c\\w+e","JSESSIONID");
        requestAtom!!.sendCookies = cookies
        requestAtom!!.setSuccessors(IntArray(0))
        requestAtom!!.predecessorCount = 0
        requestAtom!!.repeat = 1
        requestAtom!!.setParent(story)
        val params = HashMap<String,String>()
        params.put("cookie","1234")
        runBlocking {requestAtom!!.run(params)}
        MatcherAssert.assertThat(
            cookiehandler.lastCookie,
            Matchers.containsString("1234")
        )

    }

    @Test
    fun setsMultipleCookies(){
        val story = UserStory()
        story.name = "story"
        val test = de.hpi.tdgt.test.Test()
        story.parent = test
        requestAtom!!.verb = "GET"
        requestAtom!!.addr = "http://localhost:9000/cookie"
        val cookies = HashMap<String, String>()
        cookies.put("cookie","JSESSIONID");
        cookies.put("cookie2","JSESSIONID2");
        requestAtom!!.sendCookies = cookies
        requestAtom!!.setSuccessors(IntArray(0))
        requestAtom!!.predecessorCount = 0
        requestAtom!!.repeat = 1
        requestAtom!!.setParent(story)
        val params = HashMap<String,String>()
        params.put("cookie","1234")
        params.put("cookie2","5678")
        runBlocking {requestAtom!!.run(params)}
        MatcherAssert.assertThat(
            cookiehandler.lastCookie,
            Matchers.containsString("1234")
        )
        MatcherAssert.assertThat(
            cookiehandler.lastCookie,
            Matchers.containsString("5678")
        )

    }
    @Test
    public fun parsesHTML(){
        val tokens = HashMap<String, String>()
        tokens.put("_csrf","token")
        requestAtom!!.tokenNames = tokens
        requestAtom!!.extractCSRFTokens(String(Utils().signupHtml.readAllBytes()))
        MatcherAssert.assertThat(requestAtom!!.knownParams,  Matchers.hasEntry(Matchers.equalTo("token"), Matchers.equalTo("90730144-6e10-4f94-8f6a-8de3353f40f5")))
    }

    @Test
    public fun parsesHTMLFromEndpoint(){
        val tokens = HashMap<String, String>()
        tokens.put("_csrf","token")
        requestAtom!!.tokenNames = tokens
        val story = UserStory()
        story.name = "story"
        val test = de.hpi.tdgt.test.Test()
        story.parent = test
        requestAtom!!.verb = "GET"
        requestAtom!!.addr = "http://localhost:9000/html"
        val cookies = HashMap<String, String>()
        cookies.put("cookie","JSESSIONID");
        cookies.put("cookie2","JSESSIONID2");
        requestAtom!!.sendCookies = cookies
        requestAtom!!.setSuccessors(IntArray(0))
        requestAtom!!.predecessorCount = 0
        requestAtom!!.repeat = 1
        requestAtom!!.setParent(story)
        runBlocking {requestAtom!!.run(HashMap())}
        MatcherAssert.assertThat(requestAtom!!.knownParams,  Matchers.hasEntry(Matchers.equalTo("token"), Matchers.equalTo("90730144-6e10-4f94-8f6a-8de3353f40f5")))
    }

    @Test
    public fun parsesHTMLWithCustomXPATH(){
        val xpaths = HashMap<String, String>()
        xpaths.put("//input[@id='email']/@placeholder","user")
        requestAtom!!.xpaths = xpaths
        requestAtom!!.extractCSRFTokens(String(Utils().signupHtml.readAllBytes()))
        MatcherAssert.assertThat(requestAtom!!.knownParams,  Matchers.hasEntry(Matchers.equalTo("user"), Matchers.equalTo("username@jackrutorial.com")))
    }


    @Test
    public fun parsesHTMLWithCustomXPATHFromEndpoint(){
        val xpaths = HashMap<String, String>()
        xpaths.put("//input[@id='email']/@placeholder","user")
        requestAtom!!.xpaths = xpaths
        val story = UserStory()
        story.name = "story"
        val test = de.hpi.tdgt.test.Test()
        story.parent = test
        requestAtom!!.verb = "GET"
        requestAtom!!.addr = "http://localhost:9000/html"
        requestAtom!!.setSuccessors(IntArray(0))
        requestAtom!!.predecessorCount = 0
        requestAtom!!.repeat = 1
        requestAtom!!.setParent(story)
        runBlocking {requestAtom!!.run(HashMap())}
        MatcherAssert.assertThat(requestAtom!!.knownParams,  Matchers.hasEntry(Matchers.equalTo("user"), Matchers.equalTo("username@jackrutorial.com")))
    }
    @Test
    public fun parsesHTMLWithCustomXPATHEnquotedFromEndpoint(){
        val xpaths = HashMap<String, String>()
        xpaths.put("//ul[li=\$val]//a/@href","abc")
        requestAtom!!.xpaths = xpaths
        val story = UserStory()
        story.name = "story"
        val test = de.hpi.tdgt.test.Test()
        story.parent = test
        requestAtom!!.verb = "GET"
        requestAtom!!.addr = "http://localhost:9000/html"
        requestAtom!!.setSuccessors(IntArray(0))
        requestAtom!!.predecessorCount = 0
        requestAtom!!.repeat = 1
        requestAtom!!.setParent(story)
        var params = HashMap<String, String>()
        params.put("val","I've not done any javascript at all and I' trying to sum up values from the select class. can get both of them displayed, but not summed up. Could anyone explain why I'm getting the \"[object HTMLParagraphElement]\" as the answer? Thank you")
        runBlocking {requestAtom!!.run(params)}
        MatcherAssert.assertThat(requestAtom!!.knownParams,  Matchers.hasEntry(Matchers.equalTo("abc"), Matchers.equalTo("/posts/30/delete")))
    }

    @Test
    fun canCloneTokens(){
        val test = deserialize(Utils().getRequestExampleWithTokens())
        MatcherAssert.assertThat((test.getStories()[0].getAtoms()[2].clone() as Request).tokenNames, Matchers.hasEntry(Matchers.equalTo("_csrf"), Matchers.equalTo("_csrf")))
    }

    @Test
    fun canCloneTimeAggregationFalse(){
        val test = deserialize(Utils().requestExampleWithRequestReplacement)
        val request = test.getStories()[0].getAtoms()[1] as Request
        request.timeAggregation = false
        MatcherAssert.assertThat((request.clone() as Request).timeAggregation, Matchers.equalTo(false))
    }
    @Test
    fun canCloneTimeAggregationTrue(){
        val test = deserialize(Utils().requestExampleWithRequestReplacement)
        val request = test.getStories()[0].getAtoms()[1] as Request
        request.timeAggregation = true
        MatcherAssert.assertThat((request.clone() as Request).timeAggregation, Matchers.equalTo(true))
    }
    @Test
    fun canCloneReceiveCookies(){
        val test = deserialize(Utils().getRequestExampleWithTokens())
        MatcherAssert.assertThat((test.getStories()[0].getAtoms()[2].clone() as Request).receiveCookies, Matchers.hasEntry(Matchers.equalTo("JSESSIONID"), Matchers.equalTo("JSESSIONID")))
    }
    @Test
    fun canCloneSendCookies(){
        val test = deserialize(Utils().getRequestExampleWithTokens())
        MatcherAssert.assertThat((test.getStories()[0].getAtoms()[3].clone() as Request).sendCookies, Matchers.hasEntry(Matchers.equalTo("JSESSIONID"), Matchers.equalTo("JSESSIONID")))
    }

    @Test
    fun canCloneXPaths(){
        val xpaths = HashMap<String, String>()
        xpaths.put("//input[@id='email']/@placeholder","user")
        requestAtom!!.xpaths = xpaths
        MatcherAssert.assertThat((requestAtom!!.clone() as Request).xpaths, Matchers.equalToObject(xpaths))
    }

    @Test
    @Throws(InterruptedException::class, ExecutionException::class)
    fun canReplaceInAdress() {
        val params = HashMap<String, String>()
        params["key"] = "wrong"
        params["value"] = "wrong"
        params["endpoint"] = "/auth"
        getWithAuth!!.addr = "http://localhost:9000\$endpoint"
        runBlocking{getWithAuth!!.run(params)}
        MatcherAssert.assertThat(authHandler.totalRequests, Matchers.greaterThan(0))
    }

    @Test
    @Throws(InterruptedException::class, ExecutionException::class)
    fun canReplaceInAdressWithoutSeparator() {
        val params = HashMap<String, String>()
        params["key"] = "wrong"
        params["value"] = "wrong"
        params["endpoint1"] = "/"
        params["endpoint2"] = "auth"
        getWithAuth!!.addr = "http://localhost:9000\$endpoint1\$endpoint2"
        runBlocking{getWithAuth!!.run(params)}
        MatcherAssert.assertThat(authHandler.totalRequests, Matchers.greaterThan(0))
    }

    @Test
    @Throws(InterruptedException::class, ExecutionException::class)
    fun canTransferHeader() {
        val params = HashMap<String, String>()
        params["header1"] = "value1"
        getWithAuth!!.addr = "http://localhost:9000/headers"
        val headers = HashMap<String,String>()
        headers["Header_\$header1_sent"] = "header1"
        getWithAuth!!.sendHeaders = headers
        runBlocking{getWithAuth!!.run(params)}
        MatcherAssert.assertThat(headerHandler.lastHeaders!!.getFirst("header1"), Matchers.equalTo("Header_value1_sent"))
    }

    @Test
    @Throws(InterruptedException::class, ExecutionException::class)
    fun canExtractHeaders() {
        val params = HashMap<String, String>()
        getWithAuth!!.addr = "http://localhost:9000/headers"
        val receiveHeaders = HashMap<String, String>()
        receiveHeaders.put("custom","custom")
        getWithAuth!!.receiveHeaders = receiveHeaders
        runBlocking{getWithAuth!!.run(params)}
        MatcherAssert.assertThat(getWithAuth!!.knownParams, Matchers.hasEntry("custom","CustomValue"))
    }

    @Test
    @Throws(InterruptedException::class, ExecutionException::class)
    fun canCloneHeaders() {
        val params = HashMap<String, String>()
        getWithAuth!!.addr = "http://localhost:9000/headers"
        val receiveHeaders = HashMap<String, String>()
        receiveHeaders.put("custom","custom")
        getWithAuth!!.receiveHeaders = receiveHeaders
        val headers = HashMap<String,String>()
        headers["header1"] = "Header_\$header1_sent"
        getWithAuth!!.sendHeaders = headers
        runBlocking{getWithAuth!!.run(params)}
        MatcherAssert.assertThat((getWithAuth!!.clone() as Request).receiveHeaders, Matchers.equalTo(receiveHeaders as Map<String, String>))
        MatcherAssert.assertThat((getWithAuth!!.clone() as Request).sendHeaders, Matchers.hasEntry("header1","Header_\$header1_sent"))
    }

    @Test
    @Throws(InterruptedException::class, ExecutionException::class)
    fun quotesInJsonAreEscaped() {
        val params = HashMap<String, String>()
        params["key"] = "something\""
        params["value"] = "somethingElse\""
        postWithBodyAndAssertion!!.knownParams.putAll(params)
        val replaced = postWithBodyAndAssertion!!.replaceWithKnownParams("{key: \$key}",true)
        MatcherAssert.assertThat(replaced, Matchers.equalTo("{key: \"something\\\"\"}"))
    }

}