package de.hpi.tdgt.requesthandling

import de.hpi.tdgt.test.Test
import de.hpi.tdgt.test.story.UserStory
import de.hpi.tdgt.test.time_measurement.TimeStorage
import de.hpi.tdgt.util.PropertiesReader
import io.netty.handler.codec.http.cookie.Cookie
import io.netty.handler.codec.http.cookie.DefaultCookie
import kotlinx.coroutines.future.await
import org.apache.logging.log4j.LogManager
import org.asynchttpclient.*
import java.io.IOException
import java.net.MalformedURLException
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import java.util.stream.Collectors

class RestClient {
    @Throws(IOException::class)
    suspend fun getFromEndpoint(
        story: UserStory?, recordName:String?,
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
        request.recordName = recordName
        return exchangeWithEndpoint(request)
    }

    @Throws(IOException::class)
    suspend fun getBodyFromEndpoint(
        story: UserStory?, recordName:String?,
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
        request.recordName = recordName
        return exchangeWithEndpoint(request)
    }

    @Throws(IOException::class)
    suspend fun postFormToEndpoint(
        story: UserStory?, recordName:String?,
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
        request.recordName = recordName
        return exchangeWithEndpoint(request)
    }

    @Throws(IOException::class)
    suspend fun postBodyToEndpoint(story: UserStory?, recordName:String?, testId: Long, url: URL?, receiveCookies: Array<String>,
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
        request.recordName = recordName
        return exchangeWithEndpoint(request)
    }

    @Throws(IOException::class)
    suspend fun putFormToEndpoint(
        story: UserStory?, recordName:String?,
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
        request.recordName = recordName
        return exchangeWithEndpoint(request)
    }

    @Throws(IOException::class)
    suspend fun putBodyToEndpoint(story: UserStory?, recordName:String?, testId: Long, url: URL?, receiveCookies: Array<String>,
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
        request.recordName = recordName
        return exchangeWithEndpoint(request)
    }

    @Throws(IOException::class)
    suspend fun getFromEndpointWithAuth(
        story: UserStory?, recordName:String?,
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
        request.recordName = recordName
        return exchangeWithEndpoint(request)
    }

    @Throws(IOException::class)
    suspend fun getBodyFromEndpointWithAuth(
        story: UserStory?, recordName:String?,
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
        request.recordName = recordName
        return exchangeWithEndpoint(request)
    }

    @Throws(IOException::class)
    suspend fun postFormToEndpointWithAuth(
        story: UserStory?, recordName:String?,
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
        request.recordName = recordName
        return exchangeWithEndpoint(request)
    }

    @Throws(IOException::class)
    suspend fun postBodyToEndpointWithAuth(
        story: UserStory?, recordName:String?,
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
        request.recordName = recordName
        return exchangeWithEndpoint(request)
    }

    @Throws(IOException::class)
    suspend fun putFormToEndpointWithAuth(
        story: UserStory?, recordName:String?,
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
        request.recordName = recordName
        return exchangeWithEndpoint(request)
    }

    @Throws(IOException::class)
    suspend fun putBodyToEndpointWithAuth(
        story: UserStory?, recordName:String?,
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
        request.recordName = recordName
        return exchangeWithEndpoint(request)
    }

    @Throws(IOException::class)
    suspend fun deleteFromEndpoint(
        story: UserStory?, recordName:String?,
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
        request.recordName = recordName
        request.story = story
        return exchangeWithEndpoint(request)
    }

    @Throws(IOException::class)
    suspend fun deleteFromEndpointWithAuth(
            story: UserStory?, recordName:String?,
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
        request.recordName = recordName
        return exchangeWithEndpoint(request)
    }
     //above methods are for user's convenience, this method does the actual request
    @Throws(IOException::class)
    suspend fun exchangeWithEndpoint(request: Request): RestResult? {
         //in case global connection sharing is enabled, clients of stories might be null
         val client = (request.story?.client)?:UserStory.staticClient

         //if got a connection or error occurs
         val result = RestResult()
        //append GET parameters if necessary
        if(request.url == null || request.method == null){
            return null;
        }
        val url =
            appendGetParametersToUrlIfNecessary(request.url!!, request.params, request.method!!)

        val preparedRequest = Dsl.request(request.method, url.toString())
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
        //set POST Body to contain formencoded data
        if (request.isForm && (request.method == HttpConstants.POST || request.method == HttpConstants.PUT)) {
            preparedRequest.setHeader("Content-Type", HttpConstants.APPLICATION_X_WWW_FORM_URLENCODED)
            val body = request.params?.entries?.stream()?.map { entry -> Param(entry.key, entry.value) }?.collect(Collectors.toList())
            preparedRequest.setFormParams(body)
        }
        //set POST body to what was passed
        if (!request.isForm && (request.method == HttpConstants.POST || request.method == HttpConstants.PUT || request.method == HttpConstants.GET) && request.body != null) {
            preparedRequest.setHeader("Content-Type", HttpConstants.CONTENT_TYPE_APPLICATION_JSON)
            if(request.body != null) {
                preparedRequest.setBody(request.body!!.toByteArray(StandardCharsets.UTF_8))
            }
        }

        for(cookie in request.sendCookies.entries){
            preparedRequest.addCookie(DefaultCookie(cookie.key,cookie.value))
        }
        try {
            for (header in request.sendHeaders.entries) {
                preparedRequest.setHeader(header.key, header.value)
            }
        } catch (e: Exception){
            result.errorCondition = e
        }

        //try to connect
        var retry: Int = -1

        var future:ListenableFuture<Response>? = null

        while (retry < request.retries) {
            try{
            Test.ConcurrentRequestsThrottler.instance.allowRequest()
            //Exceptions might be thrown here as well as later when waiting for the response
            try {
                future = client.executeRequest(preparedRequest)
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
        val response:Response
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
        return result;
        }
            finally{
                //created a new story for this request that also created a new client
                if(request.story == null){
                    client.close()
                }
                Test.ConcurrentRequestsThrottler.instance.requestDone()
            }

        }
         //clients created because no story was given have to be closed
         if(request.story == null){client.close()}
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
            response: Response,
            res: RestResult,
            request: Request
    ) {
        val responseData = response.responseBodyAsBytes
        //got results, store them and the time
//TODO do we want to measure the time to transfer the data? Currently we are, but we could also take the time after retrieving content length
        res.response = responseData
        res.endTime = System.nanoTime()
        res.contentType = response.contentType
        res.headers = response.headers
        res.returnCode = response.statusCode
        for(cookie in request.receiveCookies){
            var foundCookie = false;
            for(responseCookie in response.cookies){
                //accept first cookie, second one is probably a mistake
                if(cookie == responseCookie.name() && !foundCookie){
                    res.receivedCookies.put(cookie, responseCookie.value())
                    foundCookie = true;
                }
                if(cookie == responseCookie.name() && foundCookie){
                    log.warn("Duplicate cookie key $cookie")
                }
            }
        }
        storage.registerTime(
            request.method,
            request.recordName,
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