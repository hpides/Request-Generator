package de.hpi.tdgt.requesthandling

import de.hpi.tdgt.test.Test
import de.hpi.tdgt.test.time_measurement.TimeStorage
import org.apache.commons.io.IOUtils
import org.apache.commons.io.input.CountingInputStream
import org.apache.logging.log4j.LogManager
import org.asynchttpclient.DefaultAsyncHttpClientConfig
import org.asynchttpclient.Dsl
import org.asynchttpclient.ListenableFuture
import org.asynchttpclient.Response
import java.io.BufferedInputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.net.MalformedURLException
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import java.util.zip.GZIPInputStream

class RestClient {
    @Throws(IOException::class)
    fun getFromEndpoint(
        story: String?,
        testId: Long,
        url: URL?,
        getParams: Map<String, String>
    ): RestResult? {
        val request = Request()
        request.url = url
        request.params = getParams
        request.method = HttpConstants.GET
        request.story = story
        request.testId = testId
        return exchangeWithEndpoint(request)
    }

    @Throws(IOException::class)
    fun getBodyFromEndpoint(
        story: String?,
        testId: Long,
        url: URL?,
        body: String?
    ): RestResult? {
        val request = Request()
        request.url = url
        request.method = HttpConstants.GET
        request.body = body
        request.isForm = false
        request.story = story
        request.testId = testId
        return exchangeWithEndpoint(request)
    }

    @Throws(IOException::class)
    fun postFormToEndpoint(
        story: String?,
        testId: Long,
        url: URL?,
        getParams: Map<String, String>
    ): RestResult? {
        val request = Request()
        request.url = url
        request.params = getParams
        request.method = HttpConstants.POST
        request.isForm = true
        request.story = story
        request.testId = testId
        return exchangeWithEndpoint(request)
    }

    @Throws(IOException::class)
    fun postBodyToEndpoint(story: String?, testId: Long, url: URL?, body: String?): RestResult? {
        val request = Request()
        request.url = url
        request.method = HttpConstants.POST
        request.isForm = false
        request.body = body
        request.story = story
        request.testId = testId
        return exchangeWithEndpoint(request)
    }

    @Throws(IOException::class)
    fun putFormToEndpoint(
        story: String?,
        testId: Long,
        url: URL?,
        getParams: Map<String, String>
    ): RestResult? {
        val request = Request()
        request.url = url
        request.params = getParams
        request.method = HttpConstants.PUT
        request.isForm = true
        request.story = story
        request.testId = testId
        return exchangeWithEndpoint(request)
    }

    @Throws(IOException::class)
    fun putBodyToEndpoint(story: String?, testId: Long, url: URL?, body: String?): RestResult? {
        val request = Request()
        request.url = url
        request.method = HttpConstants.PUT
        request.isForm = false
        request.body = body
        request.story = story
        request.testId = testId
        return exchangeWithEndpoint(request)
    }

    @Throws(IOException::class)
    fun getFromEndpointWithAuth(
        story: String?,
        testId: Long,
        url: URL?,
        getParams: Map<String, String>,
        username: String?,
        password: String?
    ): RestResult? {
        val request = Request()
        request.url = url
        request.params = getParams
        request.method = HttpConstants.GET
        request.username = username
        request.password = password
        request.story = story
        request.testId = testId
        return exchangeWithEndpoint(request)
    }

    @Throws(IOException::class)
    fun getBodyFromEndpointWithAuth(
        story: String?,
        testId: Long,
        url: URL?,
        body: String?,
        username: String?,
        password: String?
    ): RestResult? {
        val request = Request()
        request.url = url
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
    fun postFormToEndpointWithAuth(
        story: String?,
        testId: Long,
        url: URL?,
        getParams: Map<String, String>,
        username: String?,
        password: String?
    ): RestResult? {
        val request = Request()
        request.url = url
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
    fun postBodyToEndpointWithAuth(
        story: String?,
        testId: Long,
        url: URL?,
        body: String?,
        username: String?,
        password: String?
    ): RestResult? {
        val request = Request()
        request.url = url
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
    fun putFormToEndpointWithAuth(
        story: String?,
        testId: Long,
        url: URL?,
        getParams: Map<String, String>,
        username: String?,
        password: String?
    ): RestResult? {
        val request = Request()
        request.url = url
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
    fun putBodyToEndpointWithAuth(
        story: String?,
        testId: Long,
        url: URL?,
        body: String?,
        username: String?,
        password: String?
    ): RestResult? {
        val request = Request()
        request.url = url
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
    fun deleteFromEndpoint(
        story: String?,
        testId: Long,
        url: URL?,
        getParams: Map<String, String>
    ): RestResult? {
        val request = Request()
        request.url = url
        request.params = getParams
        request.method = HttpConstants.DELETE
        request.testId = testId
        return exchangeWithEndpoint(request)
    }

    @Throws(IOException::class)
    fun deleteFromEndpointWithAuth(
            story: String?,
            testId: Long,
            url: URL?,
            getParams: Map<String, String>,
            username: String?,
            password: String?
    ): RestResult? {
        val request = Request()
        request.url = url
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
    fun exchangeWithEndpoint(request: Request): RestResult? {
        //append GET parameters if necessary
        if(request.url == null || request.method == null){
            return null;
        }
        val url =
            appendGetParametersToUrlIfNecessary(request.url!!, request.params, request.method!!)

        val client = Dsl.asyncHttpClient(DefaultAsyncHttpClientConfig.Builder().setConnectTimeout(request.connectTimeout).setReadTimeout(request.responseTimeout).setFollowRedirect(request.isFollowsRedirects).setKeepAlive(request.isSendKeepAlive))
        val preparedRequest = client.prepare(request.method, url.toString())
        var retry: Int
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
            val body = mapToURLEncodedString(request.params).toString()
            preparedRequest.setBody(body)
        }
        //set POST body to what was passed
        if (!request.isForm && (request.method == HttpConstants.POST || request.method == HttpConstants.PUT || request.method == HttpConstants.GET) && request.body != null) {
            preparedRequest.setHeader("Content-Type", HttpConstants.CONTENT_TYPE_APPLICATION_JSON)
            if(request.body != null) {
                preparedRequest.setBody(request.body!!.toByteArray(StandardCharsets.UTF_8))
            }
        }
        //got a connection
        val result = RestResult()
        //try to connect
        retry = -1

        var future:ListenableFuture<Response>? = null

        while (retry < request.retries) {
            Test.ConcurrentRequestsThrottler.instance.allowRequest()
            //Exceptions might be thrown here as well as later when waiting for the response
            try {
                future = preparedRequest.execute()
            } catch (e: Exception) {
                log.error("Could not connect to $url", e)
                result.errorCondition = e
                return result
            }
            retry++
        }
        if(future == null){
            log.error("Unknown error while sending request!")
            return result
        }
        result.startTime = start
        val response:Response
        try {
            response = future.get()
        } catch (e: Exception) {
            log.error("Could not connect to $url", e)
            result.errorCondition = e
            return result
        }
        readResponse(response, result, request)
        Test.ConcurrentRequestsThrottler.instance.requestDone()
        return result
    }

    @Throws(MalformedURLException::class)
    private fun appendGetParametersToUrlIfNecessary(
        url: URL,
        params: Map<String, String?>?,
        method: String
    ): URL { //add URL parameters
        var url = url
        if ((method == HttpConstants.GET || method == HttpConstants.DELETE) && params != null && !params.isEmpty()) {
            url = URL(url.toString() + "?" + mapToURLEncodedString(params))
        }
        return url
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
    private fun readResponse(
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
        storage.registerTime(
            request.method,
            request.url.toString(),
            res.durationNanos(),
            request.story,
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