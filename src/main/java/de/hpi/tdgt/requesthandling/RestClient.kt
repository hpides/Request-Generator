package de.hpi.tdgt.requesthandling

import akka.actor.ActorSystem
import akka.http.javadsl.Http
import akka.http.javadsl.model.HttpHeader
import akka.http.javadsl.model.HttpMethod
import akka.http.javadsl.model.HttpRequest
import akka.http.javadsl.model.HttpResponse
import akka.http.javadsl.model.headers.*
import akka.http.scaladsl.model.HttpMethods
import akka.stream.ActorMaterializer
import akka.stream.Materializer
import de.hpi.tdgt.test.Test
import de.hpi.tdgt.test.story.UserStory
import de.hpi.tdgt.test.time_measurement.TimeStorage
import de.hpi.tdgt.util.PropertiesReader
import kotlinx.coroutines.future.await
import org.apache.logging.log4j.LogManager
import org.asynchttpclient.Param
import java.io.IOException
import java.net.MalformedURLException
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.concurrent.CompletionStage
import java.util.concurrent.atomic.AtomicInteger
import java.util.stream.Collectors

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

    val system = ActorSystem.create()

     //above methods are for user's convenience, this method does the actual request
    @Throws(IOException::class)
    suspend fun exchangeWithEndpoint(request: Request): RestResult? {
         val client = (request.story?:UserStory()).client
        //append GET parameters if necessary
        if(request.url == null || request.method == null){
            return null;
        }
        val url =
            appendGetParametersToUrlIfNecessary(request.url!!, request.params, request.method!!)
        val headers = LinkedList<HttpHeader>()
        val preparedRequest = HttpRequest.create(url.toString()).withMethod(HttpMethods.getForKey(request.method!!).get() as HttpMethod)
        val start = System.nanoTime()
        //set auth header if required
        if (request.username != null && request.password != null) {
            preparedRequest.addCredentials(BasicHttpCredentials.createBasicHttpCredentials(
                request.username, request.password

            ))
        }
        //set POST Body to contain formencoded data
        if (request.isForm && (request.method == HttpConstants.POST || request.method == HttpConstants.PUT)) {
            headers.add(ContentType.parse("Content-Type", HttpConstants.APPLICATION_X_WWW_FORM_URLENCODED))
            val body = request.params?.entries?.stream()?.map { entry -> Param(entry.key, entry.value) }?.collect(Collectors.toList())
            preparedRequest.withEntity(mapToURLEncodedString(request.params).toString())
        }
        //set POST body to what was passed
        if (!request.isForm && (request.method == HttpConstants.POST || request.method == HttpConstants.PUT || request.method == HttpConstants.GET) && request.body != null) {
            headers.add(ContentType.parse("Content-Type", HttpConstants.CONTENT_TYPE_APPLICATION_JSON))
            if(request.body != null) {
                preparedRequest.withEntity(request.body!!.toByteArray(StandardCharsets.UTF_8))
            }
        }
        val cookieStr = java.lang.StringBuilder()
        var first = true
        for(cookie in request.sendCookies.entries){
            if(!first) {
                cookieStr.append("; ")
            }
            cookieStr.append(cookie.key).append("=").append(cookie.value)
        }
         headers.add(Cookie.parse(HttpConstants.HEADER_COOKIE, cookieStr.toString()))
        preparedRequest.withHeaders(headers)
        //got a connection
        val result = RestResult()
        //try to connect
        var retry: Int = -1

        var future: CompletionStage<HttpResponse>? = null
        while (retry < request.retries) {
            Test.ConcurrentRequestsThrottler.instance.allowRequest()
            //Exceptions might be thrown here as well as later when waiting for the response
            try {
                future =
                Http.get(system)
                        .singleRequest(preparedRequest)
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
        val response:HttpResponse
        try {
            if(!PropertiesReader.AsyncIO()) {
                response = future.toCompletableFuture().get()
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
    val mat: Materializer = ActorMaterializer.create(system)
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
            response: HttpResponse,
            res: RestResult,
            request: Request
    ) {
        val responseData = response.entity().toStrict(300000, mat).toCompletableFuture().await().data.toArray()
        //got results, store them and the time
//TODO do we want to measure the time to transfer the data? Currently we are, but we could also take the time after retrieving content length
        res.response = responseData
        res.endTime = System.nanoTime()
        res.contentType = response.entity().contentType.toString()
        res.returnCode = response.status().intValue()
        for(cookie in request.receiveCookies){
            var foundCookie = false;
            for(header in response.headers){
                //accept first cookie, second one is probably a mistake
                if(header.`is`(HttpConstants.HEADER_SET_COOKIE) && cookie == header.value().split("=")[0] && !foundCookie){
                    res.receivedCookies.put(cookie, header.value().split("=")[0].split(";")[0])
                    foundCookie = true;
                }
                if(header.`is`(HttpConstants.HEADER_SET_COOKIE) && cookie == header.value().split("=")[0] && foundCookie){
                    log.warn("Duplicate cookie key $cookie")
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