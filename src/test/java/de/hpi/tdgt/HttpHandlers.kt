package de.hpi.tdgt

import com.fasterxml.jackson.databind.ObjectMapper
import com.sun.net.httpserver.Headers
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpHandler
import de.hpi.tdgt.requesthandling.HttpConstants
import de.hpi.tdgt.util.Pair
import org.apache.logging.log4j.LogManager
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.UnsupportedEncodingException
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.stream.Collectors

object HttpHandlers {
    private val log =
        LogManager.getLogger(HttpHandlers::class.java)
    //Classes and methods based on https://www.codeproject.com/tips/1040097/create-a-simple-web-server-in-java-http-server
    /**
     * Reads an urlencodet query to a map.
     * @param query query
     * @param parameters target
     * @throws UnsupportedEncodingException
     */
    @Throws(UnsupportedEncodingException::class)
    fun parseQuery(
        query: String?,
        parameters: MutableMap<String, Any>
    ) {
        if (query != null) {
            val pairs = query.split("[&]".toRegex()).toTypedArray()
            for (pair in pairs) {
                val param = pair.split("[=]".toRegex()).toTypedArray()
                var key: String? = null
                var value: String? = null
                if (param.size > 0) {
                    key = URLDecoder.decode(
                        param[0],
                        System.getProperty("file.encoding")
                    )
                }
                if (param.size > 1) {
                    value = URLDecoder.decode(
                        param[1],
                        System.getProperty("file.encoding")
                    )
                }
                if (parameters.containsKey(key)) {
                    val obj = parameters[key]!!
                    if (obj is List<*>) {
                        val values =
                            obj.parallelStream()
                                .map<String>({ obj1: Any? -> obj1.toString() })
                                .collect(Collectors.toList())
                        values.add(value)
                    } else if (obj is String) {
                        val values: MutableList<String> =
                            ArrayList()
                        values.add(obj)
                        values.add(value!!)
                        parameters[key!!] = values
                    }
                } else {
                    parameters[key!!] = value!!
                }
            }
        }
    }

    abstract class HttpHandlerBase : HttpHandler {
        var requests_total = 0
        @Throws(IOException::class)
        override fun handle(he: HttpExchange) {
            requests_total++
        }

    }

    class GetHandler : HttpHandlerBase(), HttpHandler {
        var lastParameters: Map<String, Any>? = null
            private set
        var request: String? = ""
            private set

        @Throws(IOException::class)
        override fun handle(he: HttpExchange) {
            super.handle(he)
            // parse request
            val parameters: MutableMap<String, Any> =
                HashMap()
            val requestedUri = he.requestURI
            var query = requestedUri.rawQuery
            if (query != null) {
                query = URLDecoder.decode(query, StandardCharsets.UTF_8)
            }
            request = query
            parseQuery(query, parameters)
            lastParameters = parameters
            // send response
            val responseBuilder = StringBuilder()
            responseBuilder.append("Welcome!\n")
            for (key in parameters.keys) responseBuilder.append(key).append(" = ").append(
                parameters[key]
            ).append("\n")
            val response = responseBuilder.toString()
            val headers = he.responseHeaders
            headers[HttpConstants.HEADER_CONTENT_TYPE] = listOf(HttpConstants.CONTENT_TYPE_TEXT_PLAIN_UTF8)
            he.sendResponseHeaders(200, response.length.toLong())
            val os = he.responseBody
            os.write(response.toByteArray())
            os.close()
        }

    }

    class GetWithBodyHandler : HttpHandlerBase(), HttpHandler {
        var lastParameters: Map<String, Any>? = null
            private set

        @Throws(IOException::class)
        override fun handle(he: HttpExchange) {
            super.handle(he)
            // parse request
            lastParameters = HashMap()
            var headers = he.requestHeaders
            val contentType = headers.getFirst(HttpConstants.HEADER_CONTENT_TYPE)
            if (contentType == null || contentType != HttpConstants.CONTENT_TYPE_APPLICATION_JSON) {
                val message = "Missing Content Type Header!"
                headers = he.responseHeaders
                headers[HttpConstants.HEADER_CONTENT_TYPE] = listOf(HttpConstants.CONTENT_TYPE_APPLICATION_JSON)
                he.sendResponseHeaders(200, message.length.toLong())
                val os = he.responseBody
                os.write(message.toByteArray())
                os.close()
                return
            }
            val isr = InputStreamReader(he.requestBody, "utf-8")
            val br = BufferedReader(isr)
            val body = StringBuilder()
            var query = br.readLine()
            body.append(query)
            while (br.readLine().also { query = it } != null) {
                body.append(query)
            }
            // send response
            val response = body.toString()
            lastParameters = ObjectMapper().readValue(
                response,
                MutableMap::class.java
            ) as Map<String, Any>
            headers = he.responseHeaders
            headers[HttpConstants.HEADER_CONTENT_TYPE] = listOf(HttpConstants.CONTENT_TYPE_APPLICATION_JSON)
            he.sendResponseHeaders(200, response.length.toLong())
            val os = he.responseBody
            os.write(response.toByteArray())
            os.close()
        }

    }

    /**
     * Makes request URL Parameters to a JSON Object with the Request keys as keys and their values as values.
     */
    class JSONObjectGetHandler : HttpHandlerBase(), HttpHandler {
        private val allParams: MutableSet<Pair<String, String>> =
            HashSet()
        var requestsTotal = 0
        @Throws(IOException::class)
        override fun handle(he: HttpExchange) {
            super.handle(he)
            //count requests
            requestsTotal++
            // parse request
            val parameters: MutableMap<String, Any> =
                HashMap()
            val requestedUri = he.requestURI
            val query = requestedUri.rawQuery
            parseQuery(query, parameters)
            for ((key, value) in parameters) {
                allParams.add(Pair(key, value.toString()))
            }
            // send response
            val responseBuilder = StringBuilder()
            responseBuilder.append("{\n")
            var first = true
            for (key in parameters.keys) {
                if (!first) {
                    responseBuilder.append(",")
                }
                first = false
                responseBuilder.append("\"").append(key.replace("\"","\\\"")).append("\"").append(" : ").append("\"")
                    .append(parameters[key].toString().replace("\"","\\\"")).append("\"").append("\n")
            }
            if (!parameters.isEmpty()) {
                responseBuilder.append(",")
            }
            responseBuilder.append("\"id\"").append(" : ").append(40)
            responseBuilder.append("}")
            val response = responseBuilder.toString()
            val headers = he.responseHeaders
            headers[HttpConstants.HEADER_CONTENT_TYPE] = listOf(HttpConstants.CONTENT_TYPE_APPLICATION_JSON)
            he.sendResponseHeaders(200, response.length.toLong())
            val os = he.responseBody
            os.write(response.toByteArray())
            os.close()
        }

        fun getAllParams(): Set<Pair<String, String>> {
            return allParams
        }

    }

    /**
     * Makes request URL Parameters to a JSON Array of Objects with the Request keys as key for an object and their values as values.
     */
    class JSONArrayGetHandler : HttpHandlerBase(), HttpHandler {
        @Throws(IOException::class)
        override fun handle(he: HttpExchange) {
            super.handle(he)
            // parse request
            val parameters: MutableMap<String, Any> =
                HashMap()
            val requestedUri = he.requestURI
            val query = requestedUri.rawQuery
            parseQuery(query, parameters)
            // send response
            val responseBuilder = StringBuilder()
            responseBuilder.append("[\n")
            var first = true
            for (key in parameters.keys) {
                if (!first) {
                    responseBuilder.append(",")
                }
                first = false
                responseBuilder.append("{\"").append(key).append("\"").append(" : ").append("\"")
                    .append(parameters[key]).append("\"}").append("\n")
            }
            responseBuilder.append("]")
            val response = responseBuilder.toString()
            try {
                Thread.sleep(1000)
            } catch (e: InterruptedException) {
                log.error(e)
            }
            val headers = he.responseHeaders
            headers[HttpConstants.HEADER_CONTENT_TYPE] = listOf(HttpConstants.CONTENT_TYPE_APPLICATION_JSON)
            he.sendResponseHeaders(200, response.length.toLong())
            val os = he.responseBody
            os.write(response.toByteArray())
            os.close()
        }
    }

    /**
     * Expects post.
     */
    class PostHandler : HttpHandlerBase(), HttpHandler {
        var lastPostWasOkay = true
        @Throws(IOException::class)
        override fun handle(he: HttpExchange) {
            super.handle(he)
            // parse request
            val parameters: MutableMap<String, Any> =
                HashMap()
            val isr = InputStreamReader(he.requestBody, "utf-8")
            var headers = he.requestHeaders
            val contentType = headers.getFirst(HttpConstants.HEADER_CONTENT_TYPE)
            lastPostWasOkay = true
            if (contentType == null || contentType != HttpConstants.APPLICATION_X_WWW_FORM_URLENCODED) {
                lastPostWasOkay = false
                val message = "Missing Content Type Header!"
                headers = he.responseHeaders
                headers[HttpConstants.HEADER_CONTENT_TYPE] = listOf(HttpConstants.CONTENT_TYPE_APPLICATION_JSON)
                he.sendResponseHeaders(200, message.length.toLong())
                val os = he.responseBody
                os.write(message.toByteArray())
                os.close()
                return
            }
            val br = BufferedReader(isr)
            val query = br.readLine()
            parseQuery(query, parameters)
            // send response
            val responseBuilder = StringBuilder()
            for (key in parameters.keys) responseBuilder.append(key).append(" = ").append(
                parameters[key]
            ).append("\n")
            val response = responseBuilder.toString()
            headers = he.responseHeaders
            headers[HttpConstants.HEADER_CONTENT_TYPE] = listOf(HttpConstants.CONTENT_TYPE_TEXT_PLAIN_UTF8)
            he.sendResponseHeaders(200, response.length.toLong())
            val os = he.responseBody
            os.write(response.toByteArray())
            os.close()
        }
    }

    /**
     * Expects request in body.
     */
    class PostBodyHandler : HttpHandlerBase(), HttpHandler {
        private val allParameters: MutableSet<Map<*, *>> =
            HashSet()

        @Throws(IOException::class)
        override fun handle(he: HttpExchange) {
            super.handle(he)
            //a test relies on this taking 1 ms or longer
            Thread.sleep(2)
            // parse request
            var headers = he.requestHeaders
            val contentType = headers.getFirst(HttpConstants.HEADER_CONTENT_TYPE)
            if (contentType == null || contentType != HttpConstants.CONTENT_TYPE_APPLICATION_JSON) {
                val message = "Missing Content Type Header!"
                headers = he.responseHeaders
                headers[HttpConstants.HEADER_CONTENT_TYPE] = listOf(HttpConstants.CONTENT_TYPE_APPLICATION_JSON)
                he.sendResponseHeaders(200, message.length.toLong())
                val os = he.responseBody
                os.write(message.toByteArray())
                os.close()
                return
            }
            val isr = InputStreamReader(he.requestBody, "utf-8")
            val br = BufferedReader(isr)
            val body = StringBuilder()
            var query = br.readLine()
            body.append(query)
            while (br.readLine().also { query = it } != null) {
                body.append(query)
            }
            // send response
            val response = body.toString()
            allParameters.add(
                ObjectMapper().readValue(
                    response,
                    MutableMap::class.java
                )
            )
            headers = he.responseHeaders
            headers[HttpConstants.HEADER_CONTENT_TYPE] = listOf(HttpConstants.CONTENT_TYPE_APPLICATION_JSON)
            he.sendResponseHeaders(200, response.length.toLong())
            val os = he.responseBody
            os.write(response.toByteArray())
            os.close()
        }

        fun getAllParameters(): Set<Map<*, *>> {
            return allParameters
        }
    }

    /**
     * Expects requests to be authorized with username (hardcoded "user") and password (hardcoded "password").
     * Saves in the field "lastLoginWasOK" if the last login used the correct username and password.
     */
    class AuthHandler : HttpHandlerBase(), HttpHandler {
        var isLastLoginWasOK = false
            private set
        var numberFailedLogins = 0
        var totalRequests = 0
        @Throws(IOException::class)
        override fun handle(he: HttpExchange) {
            super.handle(he)
            synchronized(this) { totalRequests++ }
            isLastLoginWasOK = false
            val requestHeaders = he.requestHeaders
            var auth = requestHeaders.getFirst(HttpConstants.HEADER_AUTHORIZATION)
            if (auth != null && auth.startsWith("Basic ")) {
                auth = auth.substring(auth.indexOf("Basic ") + "Basic ".length)
            }
            log.info("Auth handler called with params " + String(Base64.getDecoder().decode(auth ?: "")))
            if (auth != null && Base64.getDecoder().decode(auth) != null && String(
                    Base64.getDecoder().decode(
                        auth
                    )
                ) == "$username:$password"
            ) {
                isLastLoginWasOK = true
                val response = "{\"message\":\"OK\"}"
                val headers = he.responseHeaders
                headers[HttpConstants.HEADER_CONTENT_TYPE] = listOf(HttpConstants.CONTENT_TYPE_TEXT_PLAIN)
                he.sendResponseHeaders(200, response.length.toLong())
                val os = he.responseBody
                os.write(response.toByteArray())
                os.close()
            } else {
                isLastLoginWasOK = false
                numberFailedLogins++
                val response = "UNAUTHORIZED"
                val headers = he.responseHeaders
                headers[HttpConstants.HEADER_CONTENT_TYPE] = listOf(HttpConstants.CONTENT_TYPE_TEXT_PLAIN)
                he.sendResponseHeaders(401, response.length.toLong())
                val os = he.responseBody
                os.write(response.toByteArray())
                os.close()
            }
        }

        companion object {
            const val username = "user"
            const val password = "pw"
        }
    }

    /**
     * Returns nothing.
     */
    class EmptyResponseHandler : HttpHandlerBase(), HttpHandler {
        @Throws(IOException::class)
        override fun handle(he: HttpExchange) {
            super.handle(he)
            val response = ""
            val headers = he.responseHeaders
            headers[HttpConstants.HEADER_CONTENT_TYPE] = listOf(HttpConstants.CONTENT_TYPE_TEXT_PLAIN)
            he.sendResponseHeaders(200, response.length.toLong())
            val os = he.responseBody
            os.write(response.toByteArray())
            os.close()
        }
    }

    /**
     * Simulates a session cookie.
     */
    class CookieResponseHandler : HttpHandlerBase(), HttpHandler {

        public var lastCookie = "";

        @Throws(IOException::class)
        override fun handle(he: HttpExchange) {
            super.handle(he)

            val requestHeaders = he.requestHeaders
            val sb = java.lang.StringBuilder();
            for( cookie in requestHeaders[HttpConstants.HEADER_COOKIE]?: emptyList<String>()){
                sb.append(cookie)
            }
            lastCookie = sb.toString()
            val response = ""
            val headers = he.responseHeaders
            headers[HttpConstants.HEADER_CONTENT_TYPE] = listOf(HttpConstants.CONTENT_TYPE_TEXT_PLAIN)
            val parameters: MutableMap<String, Any> =
                HashMap()
            val requestedUri = he.requestURI
            val query = requestedUri.rawQuery
            parseQuery(query, parameters)
            if(parameters.get("multiple")!= null){
                headers[HttpConstants.HEADER_SET_COOKIE] = listOf("JSESSIONID=1234567890; HttpOnly","SomethingElse=abc; Secure")
            }
            else {
                headers[HttpConstants.HEADER_SET_COOKIE] = listOf("JSESSIONID=1234567890; HttpOnly")
            }
            he.sendResponseHeaders(200, response.length.toLong())
            val os = he.responseBody
            os.write(response.toByteArray())
            os.close()
        }
    }

    /**
     * Makes request URL Parameters to a JSON Object with the Request keys as keys and their values as values.
     */
    class HTMLHandler : HttpHandlerBase(), HttpHandler {
        @Throws(IOException::class)
        override fun handle(he: HttpExchange) {
            super.handle(he)

            // send response
            val response = String(Utils().signupHtml.readAllBytes())
            val headers = he.responseHeaders
            headers[HttpConstants.HEADER_CONTENT_TYPE] = listOf(HttpConstants.CONTENT_TYPE_TEXT_HTML)
            he.sendResponseHeaders(200, response.length.toLong())
            val os = he.responseBody
            os.write(response.toByteArray())
            os.close()
        }

    }

    /**
     * Makes request URL Parameters to a JSON Object with the Request keys as keys and their values as values.
     */
    class CustomHeaderHandler : HttpHandlerBase(), HttpHandler {
        var lastHeaders:Headers? = null;
        @Throws(IOException::class)
        override fun handle(he: HttpExchange) {
            super.handle(he)
            lastHeaders = he.requestHeaders
            // send response
            val response = String(Utils().signupHtml.readAllBytes())
            val headers = he.responseHeaders
            headers[HttpConstants.HEADER_CONTENT_TYPE] = listOf(HttpConstants.CONTENT_TYPE_TEXT_HTML)
            headers["custom"] = listOf("CustomValue")
            he.sendResponseHeaders(200, response.length.toLong())
            val os = he.responseBody
            os.write(response.toByteArray())
            os.close()
        }

    }
}
