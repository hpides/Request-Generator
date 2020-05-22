package de.hpi.tdgt.requesthandling

import de.hpi.tdgt.HttpHandlers
import de.hpi.tdgt.RequestHandlingFramework
import de.hpi.tdgt.Utils
import de.hpi.tdgt.deserialisation.Deserializer.deserialize
import de.hpi.tdgt.test.ThreadRecycler.Companion.instance
import de.hpi.tdgt.test.story.UserStory
import de.hpi.tdgt.test.story.atom.Request
import de.hpi.tdgt.util.Pair
import jdk.jshell.spi.ExecutionControl
import lombok.SneakyThrows
import org.apache.logging.log4j.LogManager
import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.fail
import java.io.IOException
import java.lang.Thread.sleep
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException
import java.util.concurrent.Future

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TestRequestHandling : RequestHandlingFramework() {
    @Test
    @Throws(IOException::class)
    fun testSimpleRequest() = run {
        val rc = RestClient()
        val result = rc.getFromEndpoint(
            UserStory(),
            "Endpoint test",0,
            URL("http://localhost:9000/"), emptyArray(),
            HashMap(),
            HashMap()
        )
        val response = String(result!!.response, StandardCharsets.UTF_8)
        MatcherAssert.assertThat(response, Matchers.equalTo("Welcome!\n"))
    }

    @Test
    @Throws(IOException::class)
    fun testInvalidCharacter() = run {
        val rc = RestClient()
        val params = HashMap<String, String>()
        params["key"] = "It is what it is...and does what it offers"
        val result =
            rc.getFromEndpoint(
                UserStory(), "Endpoint test",0, URL("http://localhost:9000/"), emptyArray(),
                HashMap(), params
            )
        val response = String(result!!.response, StandardCharsets.UTF_8)
        MatcherAssert.assertThat(
            getHandler.request,
            Matchers.equalTo("key=It is what it is...and does what it offers")
        )
    }

    @Test
    @Throws(IOException::class)
    fun testContentType() = run {
        val rc = RestClient()
        val result = rc.getFromEndpoint(
            UserStory(),
            "Endpoint test",0,
            URL("http://localhost:9000/"), emptyArray(),
            HashMap(),
            HashMap()
        )
        MatcherAssert.assertThat(result!!.isPlainText, Matchers.`is`(true))
    }

    @Test
    @Throws(IOException::class)
    fun testContentDecoding() = run {
        val rc = RestClient()
        val result = rc.getFromEndpoint(
            UserStory(),
            "Endpoint test",0,
            URL("http://localhost:9000/"), emptyArray(),
            HashMap(),
            HashMap()
        )
        MatcherAssert.assertThat(
            result.toString(),
            Matchers.equalTo("Welcome!\n")
        )
    }

    @Test
    @Throws(IOException::class)
    fun testGETParams() = run {
        val rc = RestClient()
        val params = HashMap<String, String>()
        params["param"] = "value"
        val result =
            rc.getFromEndpoint(
                UserStory(), "Endpoint test",0, URL("http://localhost:9000/"), emptyArray(),
                HashMap(), params
            )
        MatcherAssert.assertThat(
            result.toString(),
            Matchers.stringContainsInOrder("param", "value")
        )
    }

    @Test
    @Throws(IOException::class)
    fun testGETBodyParams() = run {
        val rc = RestClient()
        val body = "{\"param\":\"value\"}"
        val result =
            rc.getBodyFromEndpoint(
                UserStory(), "Endpoint test",0, URL("http://localhost:9000/getWithBody"), emptyArray(),
                HashMap(), body
            )
        MatcherAssert.assertThat(result.toString(), Matchers.equalTo(body))
    }

    @Test
    @Throws(IOException::class)
    fun testDELETEParams() = run {
        val rc = RestClient()
        val params = HashMap<String, String>()
        params["param"] = "value"
        val result =
            rc.deleteFromEndpoint(
                UserStory(), "Endpoint test",0, URL("http://localhost:9000/"), emptyArray(),
                HashMap(), params
            )
        MatcherAssert.assertThat(
            result.toString(),
            Matchers.stringContainsInOrder("param", "value")
        )
    }

    @Test
    @Throws(IOException::class)
    fun testJSON() = run {
        val rc = RestClient()
        val params = HashMap<String, String>()
        params["param"] = "value"
        val result =
            rc.getFromEndpoint(
                UserStory(), "Endpoint test",0, URL("http://localhost:9000/jsonObject"), emptyArray(),
                HashMap(), params
            )
        MatcherAssert.assertThat(result!!.toJson()!!.isObject, Matchers.`is`(true))
    }

    //Regression test
    @Test
    @Throws(IOException::class, InterruptedException::class, ExecutionException::class)
    fun testJSONWithInteger() = run {
        val rq = Request()
        rq.addr = "http://localhost:9000/jsonObject"
        rq.requestParams = arrayOf("param")
        rq.verb = "POST"
        rq.responseJSONObject = arrayOf("id")
        rq.repeat = 1
        val params = HashMap<String, String>()
        params["param"] = "value"
        rq.run(params)
        MatcherAssert.assertThat<Map<String, String>>(
            rq.knownParams,
            Matchers.hasEntry("id", "40")
        )
    }

    @Test
    @Throws(IOException::class)
    fun testJSONArray() = run {
        val rc = RestClient()
        val params = HashMap<String, String>()
        params["param"] = "value"
        val result =
            rc.getFromEndpoint(
                UserStory(), "Endpoint test",0, URL("http://localhost:9000/jsonArray"), emptyArray(),
                HashMap(), params
            )
        MatcherAssert.assertThat(result!!.toJson()!!.isArray, Matchers.`is`(true))
    }

    @Test
    @Throws(IOException::class)
    fun testMeasuresTime() = run {
        val rc = RestClient()
        val params = HashMap<String, String>()
        params["param"] = "value"
        val result =
            rc.getFromEndpoint(
                UserStory(), "Endpoint test",0, URL("http://localhost:9000/jsonArray"), emptyArray(),
                HashMap(), params
            )
        MatcherAssert.assertThat(
            result!!.durationMillis(),
            Matchers.`is`(
                Matchers.both(Matchers.greaterThan(0L)).and(
                    Matchers.lessThan(
                        100000L
                    )
                )
            )
        )
    }

    @Test
    @Throws(IOException::class)
    fun testPOSTFormParams() = run {
        val rc = RestClient()
        val params = HashMap<String, String>()
        params["param"] = "value"
        val result =
            rc.postFormToEndpoint(
                UserStory(), "Endpoint test",0, URL("http://localhost:9000/echoPost"), emptyArray(),
                HashMap(), params
            )
        MatcherAssert.assertThat(
            result.toString(),
            Matchers.stringContainsInOrder("param = value")
        )
    }

    @Test
    @Throws(IOException::class)
    fun testPUTFormParams() = run {
        val rc = RestClient()
        val params = HashMap<String, String>()
        params["param"] = "value"
        val result =
            rc.putFormToEndpoint(
                UserStory(), "Endpoint test",0, URL("http://localhost:9000/echoPost"), emptyArray(),
                HashMap(), params
            )
        MatcherAssert.assertThat(
            result.toString(),
            Matchers.stringContainsInOrder("param = value")
        )
    }

    @Test
    @Throws(IOException::class)
    fun testPOSTBodyParams() = run {
        val rc = RestClient()
        val body = "{\"param\":\"value\"}"
        val result =
            rc.postBodyToEndpoint(
                UserStory(), "Endpoint test",0, URL("http://localhost:9000/postWithBody"), emptyArray(),
                HashMap(), body
            )
        MatcherAssert.assertThat(result.toString(), Matchers.equalTo(body))
    }

    @Test
    @Throws(IOException::class)
    fun testPUTBodyParams() = run {
        val rc = RestClient()
        val body = "{\"param\":\"value\"}"
        val result =
            rc.putBodyToEndpoint(
                UserStory(), "Endpoint test",0, URL("http://localhost:9000/postWithBody"), emptyArray(),
                HashMap(), body
            )
        MatcherAssert.assertThat(result.toString(), Matchers.equalTo(body))
    }

    @Test
    @Throws(IOException::class)
    fun testGETWithAuth() = run {
        val rc = RestClient()
        val result = rc.getFromEndpointWithAuth(
            UserStory(),
            "Endpoint test",0,
            URL("http://localhost:9000/auth"),
            emptyArray(),
            HashMap(),
            HashMap(),
            HttpHandlers.AuthHandler.username,
            HttpHandlers.AuthHandler.password
        )
        MatcherAssert.assertThat(result!!.returnCode, Matchers.equalTo(200))
    }

    @Test
    @Throws(IOException::class)
    fun testGETBodyWithAuth() = run {
        val rc = RestClient()
        val result = rc.getBodyFromEndpointWithAuth(
            UserStory(),
            "Endpoint test",0,
            URL("http://localhost:9000/auth"),
            emptyArray(),
            HashMap(),
            "\"Something\"",
            HttpHandlers.AuthHandler.username,
            HttpHandlers.AuthHandler.password
        )
    }

    @Test
    @Throws(IOException::class)
    fun testDELETEWithAuth() = run {
        val rc = RestClient()
        val result = rc.deleteFromEndpointWithAuth(
            UserStory(),
            "Endpoint test",0,
            URL("http://localhost:9000/auth"),
            emptyArray(),
            HashMap(),
            HashMap(),
            HttpHandlers.AuthHandler.username,
            HttpHandlers.AuthHandler.password
        )
        MatcherAssert.assertThat(result!!.returnCode, Matchers.equalTo(200))
    }

    @Test
    @Throws(IOException::class)
    fun testPOSTBodyWithAuth() = run {
        val rc = RestClient()
        val result = rc.postBodyToEndpointWithAuth(
            UserStory(),
            "Endpoint test",0,
            URL("http://localhost:9000/auth"),
            emptyArray(),
            HashMap(),
            "\"Something\"",
            HttpHandlers.AuthHandler.username,
            HttpHandlers.AuthHandler.password
        )
        MatcherAssert.assertThat(result!!.returnCode, Matchers.equalTo(200))
    }

    @Test
    @Throws(IOException::class)
    fun testPOSTFormWithAuth() = run {
        val rc = RestClient()
        val result = rc.postFormToEndpointWithAuth(
            UserStory(),
            "Endpoint test",0,
            URL("http://localhost:9000/auth"),
            emptyArray(),
            HashMap(),
            HashMap(),
            HttpHandlers.AuthHandler.username,
            HttpHandlers.AuthHandler.password
        )
        MatcherAssert.assertThat(result!!.returnCode, Matchers.equalTo(200))
    }

    @Test
    @Throws(IOException::class)
    fun testPUTBodyWithAuth() = run {
        val rc = RestClient()
        val result = rc.putBodyToEndpointWithAuth(
            UserStory(),
            "Endpoint test",0,
            URL("http://localhost:9000/auth"),
            emptyArray(),
            HashMap(),
            "\"Something\"",
            HttpHandlers.AuthHandler.username,
            HttpHandlers.AuthHandler.password
        )
        MatcherAssert.assertThat(result!!.returnCode, Matchers.equalTo(200))
    }

    @Test
    @Throws(IOException::class)
    fun testPUTFormWithAuth() = run {
        val rc = RestClient()
        val result = rc.putFormToEndpointWithAuth(
            UserStory(),
            "Endpoint test",0,
            URL("http://localhost:9000/auth"),
            emptyArray(),
            HashMap(),
            HashMap(),
            HttpHandlers.AuthHandler.username,
            HttpHandlers.AuthHandler.password
        )
        MatcherAssert.assertThat(result!!.returnCode, Matchers.equalTo(200))
    }

    @Test
    @Throws(IOException::class, InterruptedException::class, ExecutionException::class)
    fun testFirstUserStory() = run {
        val test =
            deserialize(Utils().requestExampleJSON)
        //do not run second story for this time around; messes with results
        test.setStories(arrayOf(test.getStories()[0]))
        test.start()
        val params1: MutableMap<String, String> =
            HashMap()
        val params2: MutableMap<String, String> =
            HashMap()
        params1["key"] = "wrong"
        params1["value"] = "wrong"
        params2["key"] = "user"
        params2["value"] = "pw"
        //should have seen wrong and wrong as well as user and pw
        MatcherAssert.assertThat(
            "GetHandler should have received key=wrong.",
            jsonObjectGetHandler.getAllParams().contains(
                Pair(
                    "key",
                    "wrong"
                )
            ),
            Matchers.`is`(true)
        )
        MatcherAssert.assertThat(
            "GetHandler should have received value=wrong.",
            jsonObjectGetHandler.getAllParams().contains(
                Pair(
                    "value",
                    "wrong"
                )
            ),
            Matchers.`is`(true)
        )
        MatcherAssert.assertThat(
            "GetHandler should have received key=user.",
            jsonObjectGetHandler.getAllParams().contains(
                Pair(
                    "key",
                    "user"
                )
            ),
            Matchers.`is`(true)
        )
        MatcherAssert.assertThat(
            "GetHandler should have received value=pw.",
            jsonObjectGetHandler.getAllParams().contains(
                Pair(
                    "value",
                    "pw"
                )
            ),
            Matchers.`is`(true)
        )
        //should have seen wrong and wrong as well as user and pw
        MatcherAssert.assertThat(
            "GetHandler should have received key=wrong and value=wrong.",
            postBodyHandler.getAllParameters().contains(params1),
            Matchers.`is`(true)
        )
        MatcherAssert.assertThat(
            "GetHandler should have received key=user and value=pw.",
            postBodyHandler.getAllParameters().contains(params2),
            Matchers.`is`(true)
        )
        //repeated 7 times, only one thread has correct data
        MatcherAssert.assertThat(authHandler.numberFailedLogins, Matchers.`is`(6))
    }
    @Test
    @Throws(IOException::class, InterruptedException::class, ExecutionException::class)
    fun testFirstUserStoryWithConnectionPooling() = run {
        val test =
            deserialize(Utils().requestExampleJSON)
        test.noSession = true
        //do not run second story for this time around; messes with results
        test.setStories(arrayOf(test.getStories()[0]))
        test.start()
        val params1: MutableMap<String, String> =
            HashMap()
        val params2: MutableMap<String, String> =
            HashMap()
        params1["key"] = "wrong"
        params1["value"] = "wrong"
        params2["key"] = "user"
        params2["value"] = "pw"
        //should have seen wrong and wrong as well as user and pw
        MatcherAssert.assertThat(
            "GetHandler should have received key=wrong.",
            jsonObjectGetHandler.getAllParams().contains(
                Pair(
                    "key",
                    "wrong"
                )
            ),
            Matchers.`is`(true)
        )
        MatcherAssert.assertThat(
            "GetHandler should have received value=wrong.",
            jsonObjectGetHandler.getAllParams().contains(
                Pair(
                    "value",
                    "wrong"
                )
            ),
            Matchers.`is`(true)
        )
        MatcherAssert.assertThat(
            "GetHandler should have received key=user.",
            jsonObjectGetHandler.getAllParams().contains(
                Pair(
                    "key",
                    "user"
                )
            ),
            Matchers.`is`(true)
        )
        MatcherAssert.assertThat(
            "GetHandler should have received value=pw.",
            jsonObjectGetHandler.getAllParams().contains(
                Pair(
                    "value",
                    "pw"
                )
            ),
            Matchers.`is`(true)
        )
        //should have seen wrong and wrong as well as user and pw
        MatcherAssert.assertThat(
            "GetHandler should have received key=wrong and value=wrong.",
            postBodyHandler.getAllParameters().contains(params1),
            Matchers.`is`(true)
        )
        MatcherAssert.assertThat(
            "GetHandler should have received key=user and value=pw.",
            postBodyHandler.getAllParameters().contains(params2),
            Matchers.`is`(true)
        )
        //repeated 7 times, only one thread has correct data
        MatcherAssert.assertThat(authHandler.numberFailedLogins, Matchers.`is`(6))
    }

    @Test
    @Throws(IOException::class, InterruptedException::class, ExecutionException::class)
    fun testUserStoryAgainstTestServer() {
        val test =
            deserialize(Utils().requestExampleJSON)
        test.start()
        //repeated 7 times in first, 3 times 10 times in second story; only first value in corresponding table is correct
//in one scenario, one instance of first story gets correct param, executes once
//in other scenario, one instance of second story gets correct param; executes 10 times
        MatcherAssert.assertThat(
            authHandler.numberFailedLogins,
            Matchers.anyOf(Matchers.`is`(36), Matchers.`is`(27))
        )
    }

    @Test
    @Throws(IOException::class, InterruptedException::class, ExecutionException::class)
    fun testUserStoryWithChangedIDsAgainstTestServer() {
        val test =
            deserialize(Utils().requestExampleWithNonIndexIDsJSON)
        test.start()
        //repeated 7 times in first, 3 times 10 times in second story; only first value in corresponding table is correct
//in one scenario, one instance of first story gets correct param, executes once
//in other scenario, one instance of second story gets correct param; executes 10 times
        MatcherAssert.assertThat(
            authHandler.numberFailedLogins,
            Matchers.anyOf(Matchers.`is`(36), Matchers.`is`(27))
        )
    }

    @Test
    @Throws(IOException::class, InterruptedException::class, ExecutionException::class)
    fun testUserStoryWithRepeatAgainstTestServer() {
        val test =
            deserialize(Utils().requestExampleWithRepeatJSON)
        test.start()
        // GET with auth is 10 times executed per story, only once with the right credentials
        MatcherAssert.assertThat(authHandler.numberFailedLogins, Matchers.`is`(test.repeat * 9))
    }

    @Test
    @Throws(IOException::class, InterruptedException::class, ExecutionException::class)
    fun testUserStoryAgainstTestServerWithScaleFactor() {
        val test =
            deserialize(Utils().requestExampleJSON)
        test.start()
        MatcherAssert.assertThat(jsonObjectGetHandler.requestsTotal, Matchers.`is`(7))
    }

    @Test
    @Throws(IOException::class, InterruptedException::class, ExecutionException::class)
    fun testUserStoryAgainstTestServerWithScaleFactorAndRepeat() {
        val test =
            deserialize(Utils().requestExampleJSON)
        test.start()
        //10*0.7 times first story executed, calls this once
//10*0.3 times second story executed, calls this ten times
        MatcherAssert.assertThat(authHandler.totalRequests, Matchers.`is`(7 + 3 * 10))
    }

    @Test
    @Throws(InterruptedException::class, IOException::class, ExecutionException::class)
    fun testNoMoreInstancesPerSecondThanSetAreActive() {
        val start = System.currentTimeMillis()
        val test =
            deserialize(Utils().requestExampleJSON)
        //should not take forever
        test.activeInstancesPerSecond = 2
        test.start()
        val end = System.currentTimeMillis()
        val instances_total =
            test.scaleFactor * Arrays.stream(test.getStories()).mapToDouble(UserStory::scalePercentage).sum()
        val duration_seconds = (end - start) / 1000.0
        log.info("Total instances: $instances_total")
        val activeInstancesPerSecond = instances_total / duration_seconds
        log.info("Requests per second: $activeInstancesPerSecond")
        //maximum number of active instances per second, accounting for "bad luck"
        MatcherAssert.assertThat(
            activeInstancesPerSecond,
            Matchers.lessThanOrEqualTo(1.0 + test.activeInstancesPerSecond)
        )
    }

    private val sendRequest: Runnable = object : Runnable {
        private val parralelRequests = 0
        @SneakyThrows
        override fun run() {
            val rc = RestClient()
            run {
                rc.postFormToEndpointWithAuth(
                    UserStory(),
                    "Endpoint test",0,
                    URL("http://localhost:9000/auth"),
                    emptyArray(),
                    HashMap(),
                    HashMap(),
                    HttpHandlers.AuthHandler.username,
                    HttpHandlers.AuthHandler.password
                )
            }
        }
    }

    @Test
    @Throws(
        InterruptedException::class,
        ExecutionException::class,
        ExecutionControl.NotImplementedException::class
    )
    fun testNoMoreRequestsInParallelThanSetAreFired() = run {
        try {
            val parallelRequests = 10
            de.hpi.tdgt.test.Test.ConcurrentRequestsThrottler.instance.setMaxParallelRequests(parallelRequests)
            sleep(1000)
            val futures = Vector<Thread>()
            for (i in 0 until parallelRequests * 10) {
                futures.add(Thread.startVirtualThread { sendRequest.run() })
            }
            for (future in futures) {
                future.join()
            }
            MatcherAssert.assertThat(
                de.hpi.tdgt.test.Test.ConcurrentRequestsThrottler.instance.maximumParallelRequests,
                Matchers.lessThanOrEqualTo(parallelRequests + 2)
            )
        } catch (e: Exception) {
            log.error(e)
            fail(e)
        }
    }

    @Test
    @Throws(InterruptedException::class, IOException::class, ExecutionException::class)
    fun testNoMoreRequestsPerSecondThanSetAreFired() {
        val test =
            deserialize(Utils().requestExampleWithManyParallelRequests)
        test.start()
        //in bad situations, 2 requests more are fired
        MatcherAssert.assertThat(
            de.hpi.tdgt.test.Test.ConcurrentRequestsThrottler.instance.maximumParallelRequests,
            Matchers.lessThanOrEqualTo(test.maximumConcurrentRequests + 2)
        )
    }

    companion object {
        private val log = LogManager.getLogger(
            TestRequestHandling::class.java
        )
    }
}