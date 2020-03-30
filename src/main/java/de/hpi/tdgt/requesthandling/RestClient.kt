package de.hpi.tdgt.requesthandling

import de.hpi.tdgt.test.Test
import de.hpi.tdgt.test.story.UserStory
import de.hpi.tdgt.test.time_measurement.TimeStorage
import de.hpi.tdgt.util.PropertiesReader
import kotlinx.coroutines.future.await
import org.apache.logging.log4j.LogManager
import java.io.IOException
import java.net.*
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.time.Duration
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicInteger
import java.util.stream.Collectors
import kotlin.collections.HashMap

class RestClient {
    @Throws(IOException::class)
    suspend fun getFromEndpoint(
        story: UserStory?,
        testId: Long,
        url: URL?,
        receiveCookies: Array<String>,
        sendCookies: Map<String, String>,
        getParams: Map<String, String>
    ): RestResult? {
        val request = Request()
        request.url = url
        request.receiveCookies = receiveCookies
        request.sendCookies = sendCookies
        request.receiveCookies = receiveCookies
        request.sendCookies = sendCookies
        request.params = getParams
        request.method = HttpConstants.GET
        request.story = story
        request.testId = testId
        return exchangeWithEndpoint(request)
    }

    @Throws(IOException::class)
    suspend fun getBodyFromEndpoint(
        story: UserStory?,
        testId: Long,
        url: URL?,
        receiveCookies: Array<String>,
        sendCookies: Map<String, String>,
        body: String?
    ): RestResult? {
        val request = Request()
        request.url = url
        request.receiveCookies = receiveCookies
        request.sendCookies = sendCookies
        request.method = HttpConstants.GET
        request.body = body
        request.isForm = false
        request.story = story
        request.testId = testId
        return exchangeWithEndpoint(request)
    }

    @Throws(IOException::class)
    suspend fun postFormToEndpoint(
        story: UserStory?,
        testId: Long,
        url: URL?,
        receiveCookies: Array<String>,
        sendCookies: Map<String, String>,
        getParams: Map<String, String>
    ): RestResult? {
        val request = Request()
        request.url = url
        request.receiveCookies = receiveCookies
        request.sendCookies = sendCookies
        request.params = getParams
        request.method = HttpConstants.POST
        request.isForm = true
        request.story = story
        request.testId = testId
        return exchangeWithEndpoint(request)
    }

    @Throws(IOException::class)
    suspend fun postBodyToEndpoint(story: UserStory?, testId: Long, url: URL?, receiveCookies: Array<String>,
        sendCookies: Map<String, String>, body: String?): RestResult? {
        val request = Request()
        request.url = url
        request.receiveCookies = receiveCookies
        request.sendCookies = sendCookies
        request.method = HttpConstants.POST
        request.isForm = false
        request.body = body
        request.story = story
        request.testId = testId
        return exchangeWithEndpoint(request)
    }

    @Throws(IOException::class)
    suspend fun putFormToEndpoint(
        story: UserStory?,
        testId: Long,
        url: URL?,
        receiveCookies: Array<String>,
        sendCookies: Map<String, String>,
        getParams: Map<String, String>
    ): RestResult? {
        val request = Request()
        request.url = url
        request.receiveCookies = receiveCookies
        request.sendCookies = sendCookies
        request.params = getParams
        request.method = HttpConstants.PUT
        request.isForm = true
        request.story = story
        request.testId = testId
        return exchangeWithEndpoint(request)
    }

    @Throws(IOException::class)
    suspend fun putBodyToEndpoint(story: UserStory?, testId: Long, url: URL?, receiveCookies: Array<String>,
        sendCookies: Map<String, String>, body: String?): RestResult? {
        val request = Request()
        request.url = url
        request.receiveCookies = receiveCookies
        request.sendCookies = sendCookies
        request.method = HttpConstants.PUT
        request.isForm = false
        request.body = body
        request.story = story
        request.testId = testId
        return exchangeWithEndpoint(request)
    }

    @Throws(IOException::class)
    suspend fun getFromEndpointWithAuth(
        story: UserStory?,
        testId: Long,
        url: URL?,
        receiveCookies: Array<String>,
        sendCookies: Map<String, String>,
        getParams: Map<String, String>,
        username: String?,
        password: String?
    ): RestResult? {
        val request = Request()
        request.url = url
        request.receiveCookies = receiveCookies
        request.sendCookies = sendCookies
        request.params = getParams
        request.method = HttpConstants.GET
        request.username = username
        request.password = password
        request.story = story
        request.testId = testId
        return exchangeWithEndpoint(request)
    }

    @Throws(IOException::class)
    suspend fun getBodyFromEndpointWithAuth(
        story: UserStory?,
        testId: Long,
        url: URL?,
        receiveCookies: Array<String>,
        sendCookies: Map<String, String>,
        body: String?,
        username: String?,
        password: String?
    ): RestResult? {
        val request = Request()
        request.url = url
        request.receiveCookies = receiveCookies
        request.sendCookies = sendCookies
        request.method = HttpConstants.GET
        request.body = body
        request.isForm = false
        request.username = username
        request.password = password
        request.story = story
        request.testId = testId
        return exchangeWithEndpoint(request)
    }

    @Throws(IOException::class)
    suspend fun postFormToEndpointWithAuth(
        story: UserStory?,
        testId: Long,
        url: URL?,
        receiveCookies: Array<String>,
        sendCookies: Map<String, String>,
        getParams: Map<String, String>,
        username: String?,
        password: String?
    ): RestResult? {
        val request = Request()
        request.url = url
        request.receiveCookies = receiveCookies
        request.sendCookies = sendCookies
        request.params = getParams
        request.method = HttpConstants.POST
        request.isForm = true
        request.username = username
        request.password = password
        request.story = story
        request.testId = testId
        return exchangeWithEndpoint(request)
    }

    @Throws(IOException::class)
    suspend fun postBodyToEndpointWithAuth(
        story: UserStory?,
        testId: Long,
        url: URL?,
        receiveCookies: Array<String>,
        sendCookies: Map<String, String>,
        body: String?,
        username: String?,
        password: String?
    ): RestResult? {
        val request = Request()
        request.url = url
        request.receiveCookies = receiveCookies
        request.sendCookies = sendCookies
        request.method = HttpConstants.POST
        request.isForm = false
        request.body = body
        request.username = username
        request.password = password
        request.story = story
        request.testId = testId
        return exchangeWithEndpoint(request)
    }

    @Throws(IOException::class)
    suspend fun putFormToEndpointWithAuth(
        story: UserStory?,
        testId: Long,
        url: URL?,
        receiveCookies: Array<String>,
        sendCookies: Map<String, String>,
        getParams: Map<String, String>,
        username: String?,
        password: String?
    ): RestResult? {
        val request = Request()
        request.url = url
        request.receiveCookies = receiveCookies
        request.sendCookies = sendCookies
        request.params = getParams
        request.method = HttpConstants.PUT
        request.isForm = true
        request.username = username
        request.password = password
        request.story = story
        request.testId = testId
        return exchangeWithEndpoint(request)
    }

    @Throws(IOException::class)
    suspend fun putBodyToEndpointWithAuth(
        story: UserStory?,
        testId: Long,
        url: URL?,
        receiveCookies: Array<String>,
        sendCookies: Map<String, String>,
        body: String?,
        username: String?,
        password: String?
    ): RestResult? {
        val request = Request()
        request.url = url
        request.receiveCookies = receiveCookies
        request.sendCookies = sendCookies
        request.method = HttpConstants.PUT
        request.isForm = false
        request.body = body
        request.username = username
        request.password = password
        request.story = story
        request.testId = testId
        return exchangeWithEndpoint(request)
    }

    @Throws(IOException::class)
    suspend fun deleteFromEndpoint(
        story: UserStory?,
        testId: Long,
        url: URL?,
        receiveCookies: Array<String>,
        sendCookies: Map<String, String>,
        getParams: Map<String, String>
    ): RestResult? {
        val request = Request()
        request.url = url
        request.receiveCookies = receiveCookies
        request.sendCookies = sendCookies
        request.params = getParams
        request.method = HttpConstants.DELETE
        request.testId = testId
        request.story = story
        return exchangeWithEndpoint(request)
    }

    @Throws(IOException::class)
    suspend fun deleteFromEndpointWithAuth(
            story: UserStory?,
            testId: Long,
            url: URL?,
            receiveCookies: Array<String>,
            sendCookies: Map<String, String>,
            getParams: Map<String, String>,
            username: String?,
            password: String?
    ): RestResult? {
        val request = Request()
        request.url = url
        request.receiveCookies = receiveCookies
        request.sendCookies = sendCookies
        request.params = getParams
        request.method = HttpConstants.DELETE
        request.username = username
        request.password = password
        request.story = story
        request.testId = testId
        return exchangeWithEndpoint(request)
    }

        //above methods are for user's convenience, this method does the actual request
    @Throws(IOException::class)
    suspend fun exchangeWithEndpoint(request: Request): RestResult? {
         //val client = (request.story?:UserStory()).client
        //append GET parameters if necessary
        if(request.url == null || request.method == null){
            return null;
        }
        val url =
            appendGetParametersToUrlIfNecessary(request.url!!, request.params, request.method!!)
        var contentType = ""
        var body = ""
        //set POST Body to contain formencoded data
        if (request.isForm && (request.method == HttpConstants.POST || request.method == HttpConstants.PUT)) {
            contentType = HttpConstants.APPLICATION_X_WWW_FORM_URLENCODED
            body = mapToURLEncodedString(request.params).toString()

        }
        //set POST body to what was passed
        if (!request.isForm && (request.method == HttpConstants.POST || request.method == HttpConstants.PUT || request.method == HttpConstants.GET) && request.body != null) {
            contentType = HttpConstants.CONTENT_TYPE_APPLICATION_JSON
            if(request.body != null) {
               body = request.body!!
            }
        }

        val preparedRequest = HttpRequest.newBuilder().method(request.method, HttpRequest.BodyPublishers.ofString(body))
        preparedRequest.uri(url.toURI())
        preparedRequest.setHeader(HttpConstants.HEADER_CONTENT_TYPE, contentType)
        val start = System.nanoTime()
        //set auth header if required
        if (request.username != null && request.password != null) {
            preparedRequest.setHeader(
                HttpConstants.HEADER_AUTHORIZATION,
                "Basic " + Base64.getEncoder().encodeToString(
                    (request.username + ":" + request.password).toByteArray(StandardCharsets.UTF_8)
                )
            )
        }



        var first = true
        val cookies = StringBuilder()
        for(cookie in request.sendCookies.entries){
            if(first){
                first = false
            }
            else{
                cookies.append("; ")
            }

            cookies.append(cookie.key).append("=").append(cookie.value)
        }
        preparedRequest.setHeader(HttpConstants.HEADER_COOKIE,cookies.toString())

        //got a connection
        val result = RestResult()
        //try to connect
        var retry: Int = -1

        var future: CompletableFuture<HttpResponse<String>>?

        while (retry < request.retries) {
            Test.ConcurrentRequestsThrottler.instance.allowRequest()
            //Exceptions might be thrown here as well as later when waiting for the response
            try {
                future = (request.story?:UserStory()).client.sendAsync(preparedRequest.build(), HttpResponse.BodyHandlers.ofString())
            } catch (e: Exception) {
                log.error("Could not connect to $url", e)
                result.errorCondition = e
                retry ++
                continue
            }
        if(future == null){
            log.error("Unknown error while sending request!")
            retry++
            continue
        }
        result.startTime = start
        val response:HttpResponse<String>
        try {
            if(!PropertiesReader.AsyncIO()) {
                response = future.get()
            }else{
                response = future.toCompletableFuture().await()
            }
        } catch (e: Exception) {
            log.error("Could not connect to $url", e)
            result.errorCondition = e
            retry ++
            continue
        }
        readResponse(response, result, request)
        Test.ConcurrentRequestsThrottler.instance.requestDone()
            return result
        }
        return result
    }

    @Throws(MalformedURLException::class)
    private fun appendGetParametersToUrlIfNecessary(
        url: URL,
        params: Map<String, String?>?,
        method: String
    ): URL { //add URL parameters
        var requestURL = url
        if ((method == HttpConstants.GET || method == HttpConstants.DELETE) && params != null && !params.isEmpty()) {
            requestURL = URL(requestURL.toString() + "?" + mapToURLEncodedString(params))
        }
        return requestURL
    }

    private fun mapToURLEncodedString(params: Map<String, String?>?): StringBuilder {
        val finalURL = StringBuilder()
        if (params != null && !params.isEmpty()) {
            var firstParam = true
            for ((key1, value) in params) {
                if (!firstParam) {
                    finalURL.append("&")
                }
                //error handling
                if (value == null) {
                    log.error("No value!")
                    continue
                }
                firstParam = false
                finalURL.append(URLEncoder.encode(key1, StandardCharsets.UTF_8))
                finalURL.append("=")
                finalURL.append(URLEncoder.encode(value, StandardCharsets.UTF_8))
            }
        }
        return finalURL
    }

    /**
     * Taken from jmeter.
     * Reads the response from the URL connection.
     *
     * @param response URL from which to read response
     * @param res  [RestResult] to read response into
     * @return response content
     * @throws IOException if an I/O exception occurs
     */
    @Throws(IOException::class)
    private suspend fun readResponse(
            response: HttpResponse<String>,
            res: RestResult,
            request: Request
    ) {
        val responseData = response.body()
        //got results, store them and the time
//TODO do we want to measure the time to transfer the data? Currently we are, but we could also take the time after retrieving content length
        res.response = responseData.toByteArray()
        res.endTime = System.nanoTime()
        res.contentType = response.headers().firstValue(HttpConstants.HEADER_CONTENT_TYPE).orElse(HttpConstants.CONTENT_TYPE_TEXT_PLAIN)
        res.returnCode = response.statusCode()
        for(cookie in request.receiveCookies){
            var foundCookie = false;
            for(header in response.headers().allValues(HttpConstants.HEADER_SET_COOKIE)){
                //accept first cookie, second one is probably a mistake
                if(cookie == header.split("=")[0] && foundCookie){
                    log.warn("Duplicate cookie key $cookie")
                }
                if(cookie == header.split("=")[0] && !foundCookie){
                    res.receivedCookies[cookie] = header
                    foundCookie = true;
                }
            }
        }
        storage.registerTime(
            request.method,
            request.url.toString(),
            res.durationNanos(),
            request.story?.name,
            request.testId
        )
        log.info("Request took " + res.durationMillis() + " ms.")
        requestsSent.incrementAndGet()
    }

    companion object {
        private val log =
            LogManager.getLogger(RestClient::class.java)
        private val storage = TimeStorage.getInstance()
        /**
         * Counts how many requests the application as a whole sent. Resetted each time a test is over.
         */
        @JvmField
        var requestsSent = AtomicInteger(0)
    }
}

class NoopCookieHandler : CookieHandler() {
    override fun put(uri: URI?, responseHeaders: MutableMap<String, MutableList<String>>?) {
        //NOOP
    }

    override fun get(uri: URI?, requestHeaders: MutableMap<String, MutableList<String>>?): MutableMap<String, MutableList<String>> {
        //NOOP
        return HashMap()
    }

}
