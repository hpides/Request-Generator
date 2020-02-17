package de.hpi.tdgt.requesthandling

import de.hpi.tdgt.test.Test
import de.hpi.tdgt.test.time_measurement.TimeStorage
import org.apache.commons.io.IOUtils
import org.apache.commons.io.input.CountingInputStream
import org.apache.logging.log4j.LogManager
import java.io.BufferedInputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.net.*
import java.nio.charset.StandardCharsets
import java.util.*
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
    fun exchangeWithEndpoint(request: Request): RestResult? { //append GET parameters if necessary
        val url =
            appendGetParametersToUrlIfNecessary(request.url!!, request.params, request.method!!)
        //set given properties
        val httpURLConnection = prepareHttpUrlConnection(
            url,
            request.method!!,
            request.isFollowsRedirects,
            request.connectTimeout,
            request.responseTimeout,
            request.isSendKeepAlive
        )
        var retry: Int
        val start = System.nanoTime()
        //set auth header if required
        if (request.username != null && request.password != null) {
            httpURLConnection.setRequestProperty(
                HttpConstants.HEADER_AUTHORIZATION,
                "Basic " + Base64.getEncoder().encodeToString(
                    (request.username + ":" + request.password).toByteArray(StandardCharsets.UTF_8)
                )
            )
        }
        //set POST Body to contain formencoded data
        if (request.isForm && (request.method == HttpConstants.POST || request.method == HttpConstants.PUT)) {
            httpURLConnection.setRequestProperty("Content-Type", HttpConstants.APPLICATION_X_WWW_FORM_URLENCODED)
            val body = mapToURLEncodedString(request.params).toString()
            httpURLConnection.doOutput = true
            val out = httpURLConnection.outputStream
            out.write(body.toByteArray(StandardCharsets.UTF_8))
            out.flush()
            out.close()
        }
        //set POST body to what was passed
        if (!request.isForm && (request.method == HttpConstants.POST || request.method == HttpConstants.PUT || request.method == HttpConstants.GET) && request.body != null) {
            httpURLConnection.setRequestProperty("Content-Type", HttpConstants.CONTENT_TYPE_APPLICATION_JSON)
            httpURLConnection.doOutput = true
            val out = httpURLConnection.outputStream
            out.write(request.body!!.toByteArray(StandardCharsets.UTF_8))
            out.flush()
            out.close()
        }
        //got a connection
        val result = RestResult()
        //try to connect
        retry = -1
        while (retry < request.retries) {
            try {
                Test.ConcurrentRequestsThrottler.instance.allowRequest()
                httpURLConnection.connect()
                break
            } catch (s: SocketTimeoutException) {
                log.warn("Request timeout for URL " + url.toString() + " (connect timeout was " + request.connectTimeout + ").")
                result.errorCondition = s
            } catch (e: Exception) {
                log.error("Could not connect to $url", e)
                result.errorCondition = e
                return result
            }
            retry++
        }
        //exceeded max retries
        if (retry >= request.retries) {
            return null
        }
        result.startTime = start
        readResponse(httpURLConnection, result, request)
        Test.ConcurrentRequestsThrottler.instance.requestDone()
        return result
    }

    @Throws(IOException::class)
    private fun prepareHttpUrlConnection(
        url: URL,
        method: String,
        followsRedirects: Boolean,
        connectTimeout: Int,
        responseTimeout: Int,
        sendKeepAlive: Boolean
    ): HttpURLConnection {
        val httpURLConnection = url.openConnection() as HttpURLConnection
        httpURLConnection.instanceFollowRedirects = followsRedirects
        if (connectTimeout > 0) {
            httpURLConnection.connectTimeout = connectTimeout
        }
        if (responseTimeout > 0) {
            httpURLConnection.readTimeout = responseTimeout
        }
        if (sendKeepAlive) {
            httpURLConnection.setRequestProperty(HttpConstants.HEADER_CONNECTION, HttpConstants.KEEP_ALIVE)
        }
        //TODO headers like content type
        httpURLConnection.requestMethod = method
        return httpURLConnection
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
     * @param conn URL from which to read response
     * @param res  [RestResult] to read response into
     * @return response content
     * @throws IOException if an I/O exception occurs
     */
    @Throws(IOException::class)
    private fun readResponse(
        conn: HttpURLConnection,
        res: RestResult,
        request: Request
    ) {
        var `in`: BufferedInputStream? = null
        //final long contentLength = conn.getContentLength();
//might return nullbyte here if we do not need actual content
// works OK even if ContentEncoding is null
        val gzipped = HttpConstants.ENCODING_GZIP == conn.contentEncoding
        var instream: CountingInputStream? = null
        try {
            instream = CountingInputStream(conn.inputStream)
            `in` = if (gzipped) {
                BufferedInputStream(GZIPInputStream(instream))
            } else {
                BufferedInputStream(instream)
            }
        } catch (e: IOException) {
            if (e.cause !is FileNotFoundException) {
                val cause = e.cause
                if (cause != null) { //log.error("Cause: {}", cause.toString());
                    if (cause is Error) {
                        throw (cause as Error?)!!
                    }
                }
            }
            // Normal InputStream is not available
            val errorStream = conn.errorStream
            if (errorStream == null) {
                res.response = NULL_BA
                res.endTime = System.nanoTime()
                res.contentType = conn.contentType
                res.headers = conn.headerFields
                res.returnCode = conn.responseCode
            }
            if (log.isInfoEnabled) {
                if (conn.responseCode == 401 || conn.responseCode == 403) {
                    log.info("Error Response Code: " + conn.responseCode + "for " + request.method + " " + request.url + " for used authentication " + request.username + ":" + request.password)
                } else {
                    log.info("Error Response Code: " + conn.responseCode + "for " + request.method + " " + request.url)
                }
                if (errorStream != null) {
                    log.warn(
                        "Error Response Content: ",
                        IOUtils.toString(errorStream, "UTF-8")
                    )
                }
            }
            if (gzipped) {
                if (errorStream != null) {
                    `in` = BufferedInputStream(GZIPInputStream(errorStream))
                }
            } else {
                if (errorStream != null) {
                    `in` = BufferedInputStream(errorStream)
                }
            }
        } catch (e: Exception) {
            val cause = e.cause
            if (cause != null) {
                if (cause is Error) {
                    throw (cause as Error?)!!
                }
            }
            `in` = BufferedInputStream(conn.errorStream)
        }
        // N.B. this closes 'in'
        var responseData: ByteArray? = NULL_BA
        if (`in` != null) {
            responseData = IOUtils.toByteArray(`in`)
            `in`.close()
        }
        instream?.close()
        //got results, store them and the time
//TODO do we want to measure the time to transfer the data? Currently we are, but we could also take the time after retrieving content length
        res.response = responseData
        res.endTime = System.nanoTime()
        res.contentType = conn.contentType
        res.headers = conn.headerFields
        res.returnCode = conn.responseCode
        storage.registerTime(
            request.method,
            request.url.toString(),
            res.durationNanos(),
            request.story,
            request.testId
        )
        log.info("Request took " + res.durationMillis() + " ms.")
    }

    companion object {
        private val log =
            LogManager.getLogger(RestClient::class.java)
        private val NULL_BA = ByteArray(0) // can share these
        private val storage = TimeStorage.getInstance()
    }
}