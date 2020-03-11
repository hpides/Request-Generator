package de.hpi.tdgt.test.story.atom

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.ObjectMapper
import de.hpi.tdgt.requesthandling.RestClient
import de.hpi.tdgt.requesthandling.RestResult
import de.hpi.tdgt.test.story.atom.assertion.Assertion
import de.hpi.tdgt.test.story.atom.assertion.RequestIsSent
import org.apache.logging.log4j.LogManager
import java.io.IOException
import java.net.MalformedURLException
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.regex.Pattern

class Request : Atom() {
    var verb: String? = null
    var addr: String? = null
    /**
     * Expected usage: values of this arrays are keys. Use them as keys in a HTTP
     * Form in a Request Body, get values for these keys from passed dict.
     */
    var requestParams: Array<String> = arrayOf()
    /**
     * Expected usage: values of this arrays are keys. Get the values for these keys
     * from a response body Form and store them in the dict passed to the
     * successors.
     */
    var responseParams: Array<String> = arrayOf()
    /**
     * Expected usage: values of this arrays are keys. Use them as keys in a JSON
     * Object in a Request Body, get values for these keys from passed dict.
     */
    var requestJSONObject: String? = null
    /**
     * Expected usage: values of this arrays are keys. Get the values for these keys
     * from a response body JSON object and store them in the dict passed to the
     * successors.
     */
    var responseJSONObject: Array<String> = arrayOf()
    var basicAuth: BasicAuth? = null
    var assertions = arrayOfNulls<Assertion>(0)
    var implicitNotFailedAssertion: Assertion? = null
    @Throws(InterruptedException::class)
    override fun perform() {
        log.info("Sending request " + addr + " in Thread " + Thread.currentThread().id + "with attributes: " + knownParams)
        if (implicitNotFailedAssertion == null) {
            implicitNotFailedAssertion = RequestIsSent()
            (implicitNotFailedAssertion as RequestIsSent).name = "Request \"$name\" is sent"
        }
        when (verb) {
            "POST" -> handlePost()
            "PUT" -> handlePut()
            "DELETE" -> handleDelete()
            "GET" -> handleGet()
        }
    }

    public override fun performClone(): Atom {
        val ret = Request()
        ret.addr = addr
        ret.verb = verb
        ret.responseJSONObject = responseJSONObject
        ret.requestParams = requestParams
        ret.basicAuth = basicAuth
        ret.requestJSONObject = requestJSONObject
        ret.responseJSONObject = responseJSONObject
        ret.responseParams = responseParams
        //also stateless
        ret.assertions = assertions
        return ret
    }

    private fun handlePost() {
        if (requestJSONObject != null) {
            handlePostWithBody()
        } else {
            handlePostWithForm()
        }
    }

    private fun handlePostWithForm() {
        val params = HashMap<String, String>()
        for (key in requestParams) {
            if(knownParams[key] != null) {
                params[key] = knownParams[key]!!
            }
        }
        if (basicAuth == null) {
            try { //a few tests trigger the alternative case
                if (getParent() != null && getParent()!!.parent != null) {
                    extractResponseParams(
                        rc.postFormToEndpoint(
                            getParent()!!.name,
                            getParent()!!.parent!!.testId,
                            URL(addr),
                            params
                        )
                    )
                } else {
                    extractResponseParams(
                        rc.postFormToEndpoint(
                            "unknown",
                            0,
                            URL(addr),
                            params
                        )
                    )
                }
            } catch (e: MalformedURLException) { // TODO Auto-generated catch block
                log.error(e)
            } catch (e: IOException) { // TODO Auto-generated catch block
                log.error(e)
            }
        } else {
            try {
                extractResponseParams(
                    rc.postFormToEndpointWithAuth(
                        getParent()!!.name,
                        getParent()!!.parent!!.testId,
                        URL(addr),
                        params,
                        knownParams[basicAuth!!.user],
                        knownParams[basicAuth!!.password]
                    )
                )
            } catch (e: MalformedURLException) { // TODO Auto-generated catch block
                log.error(e)
            } catch (e: IOException) { // TODO Auto-generated catch block
                log.error(e)
            }
        }
    }

    private fun toStringMap(input: Map<*, *>): Map<String, String> {
        val ret = HashMap<String, String>()
        for (key in input.keys) {
            ret[key.toString()] = input[key].toString()
        }
        return ret
    }

    @Throws(IOException::class, JsonParseException::class, JsonMappingException::class)
    private fun extractResponseParams(result: RestResult?) {
        if (result != null && result.isJSON) {
            if (result.toJson()!!.isObject) {
                val json = String(result.response, StandardCharsets.UTF_8)
                val map: Map<*, *> =
                    om.readValue(
                        json,
                        MutableMap::class.java
                    )
                knownParams.putAll(toStringMap(map))
            } else {
                log.info("I can not handle Arrays.")
                log.info(result)
            }
        } else {
            log.warn("Not JSON! Response is ignored.")
            log.warn(result)
        }
        //in some tests, this might not exist
        if (getParent() != null && getParent()!!.parent != null) { //check assertions after request
            for (assertion in assertions) {
                assertion!!.check(result, getParent()!!.parent!!.testId)
            }
            implicitNotFailedAssertion!!.check(result, getParent()!!.parent!!.testId)
        } else {
            log.error("Can not check assertions because I do not have a parent or grandparent: $name")
        }
    }

    private fun fillEvaluationsInJson(): String? {
        var current = requestJSONObject
        for ((key, value) in knownParams) {
            current = current!!.replace("\\$" + key.toRegex(), '\"' + value + '\"')
        }
        //should show a warning
        if (Pattern.matches("\\$[a-zA-Z]*", current)) {
            val p = Pattern.compile("\\$[a-zA-Z]*")
            val m = p.matcher(current)
            val allUncompiled = HashSet<String>()
            while (m.find()) {
                allUncompiled.add(m.group())
            }
            val builder = StringBuilder()
            var first = true
            for (unmatched in allUncompiled) {
                if (!first) {
                    builder.append(',')
                }
                first = false
                builder.append(' ').append(unmatched)
            }
            log.warn("Request $name: Could not replace variable(s) $builder")
        }
        return current
    }

    private fun handlePostWithBody() {
        var jsonParams: String? = ""
        if (requestJSONObject != null) {
            jsonParams = fillEvaluationsInJson()
        }
        if (basicAuth == null) {
            try {
                extractResponseParams(
                    rc.postBodyToEndpoint(
                        getParent()!!.name,
                        getParent()!!.parent!!.testId,
                        URL(addr),
                        jsonParams
                    )
                )
            } catch (e: MalformedURLException) { // TODO Auto-generated catch block
                log.error(e)
            } catch (e: IOException) { // TODO Auto-generated catch block
                log.error(e)
            }
        } else {
            try {
                extractResponseParams(
                    rc.postBodyToEndpointWithAuth(
                        getParent()!!.name,
                        getParent()!!.parent!!.testId,
                        URL(addr),
                        jsonParams,
                        knownParams[basicAuth!!.user],
                        knownParams[basicAuth!!.password]
                    )
                )
            } catch (e: MalformedURLException) { // TODO Auto-generated catch block
                log.error(e)
            } catch (e: IOException) { // TODO Auto-generated catch block
                log.error(e)
            }
        }
    }

    private fun handlePut() {
        if (requestJSONObject != null) {
            handlePutWithBody()
        } else {
            handlePutWithForm()
        }
    }

    private fun handlePutWithForm() {
        val params = HashMap<String, String>()
        for (key in requestParams) {
            if(knownParams[key]!=null) {
                params[key] = knownParams[key]!!
            }
        }
        if (basicAuth == null) {
            try {
                extractResponseParams(
                    rc.putFormToEndpoint(
                        getParent()!!.name,
                        getParent()!!.parent!!.testId,
                        URL(addr),
                        params
                    )
                )
            } catch (e: MalformedURLException) { // TODO Auto-generated catch block
                log.error(e)
            } catch (e: IOException) { // TODO Auto-generated catch block
                log.error(e)
            }
        } else {
            try {
                extractResponseParams(
                    rc.putFormToEndpointWithAuth(
                        getParent()!!.name,
                        getParent()!!.parent!!.testId,
                        URL(addr),
                        params,
                        knownParams[basicAuth!!.user],
                        knownParams[basicAuth!!.password]
                    )
                )
            } catch (e: MalformedURLException) { // TODO Auto-generated catch block
                log.error(e)
            } catch (e: IOException) { // TODO Auto-generated catch block
                log.error(e)
            }
        }
    }

    private fun handlePutWithBody() {
        val params = HashMap<String, String>()
        if (requestJSONObject != null) { //fill out template
        }
        var jsonParams: String? = ""
        try {
            jsonParams = om.writeValueAsString(params)
        } catch (e1: JsonProcessingException) { // TODO Auto-generated catch block
            e1.printStackTrace()
        }
        if (basicAuth == null) {
            try {
                extractResponseParams(
                    rc.putBodyToEndpoint(
                        getParent()!!.name,
                        getParent()!!.parent!!.testId,
                        URL(addr),
                        jsonParams
                    )
                )
            } catch (e: MalformedURLException) { // TODO Auto-generated catch block
                log.error(e)
            } catch (e: IOException) { // TODO Auto-generated catch block
                log.error(e)
            }
        } else {
            try {
                extractResponseParams(
                    rc.putBodyToEndpointWithAuth(
                        getParent()!!.name,
                        getParent()!!.parent!!.testId,
                        URL(addr),
                        jsonParams,
                        knownParams[basicAuth!!.user],
                        knownParams[basicAuth!!.password]
                    )
                )
            } catch (e: MalformedURLException) { // TODO Auto-generated catch block
                log.error(e)
            } catch (e: IOException) { // TODO Auto-generated catch block
                log.error(e)
            }
        }
    }

    private fun handleDelete() {
        val params = HashMap<String, String>()
        for (key in requestParams) {
            if(knownParams[key]!=null) {
                params[key] = knownParams[key]!!
            }
        }
        if (basicAuth == null) {
            try {
                extractResponseParams(
                    rc.deleteFromEndpoint(
                        getParent()!!.name,
                        getParent()!!.parent!!.testId,
                        URL(addr),
                        params
                    )
                )
            } catch (e: MalformedURLException) { // TODO Auto-generated catch block
                log.error(e)
            } catch (e: IOException) { // TODO Auto-generated catch block
                log.error(e)
            }
        } else {
            try {
                extractResponseParams(
                    rc.deleteFromEndpointWithAuth(
                        getParent()!!.name,
                        getParent()!!.parent!!.testId,
                        URL(addr),
                        params,
                        knownParams[basicAuth!!.user],
                        knownParams[basicAuth!!.password]
                    )
                )
            } catch (e: MalformedURLException) { // TODO Auto-generated catch block
                log.error(e)
            } catch (e: IOException) { // TODO Auto-generated catch block
                log.error(e)
            }
        }
    }

    private fun handleGet() {
        if (requestJSONObject != null) {
            handleGetWithBody()
        } else {
            handleGetWithForm()
        }
    }

    private fun handleGetWithForm() {
        val params = HashMap<String, String>()
        for (key in requestParams) {
            if(knownParams[key]!=null) {
                params[key] = knownParams[key]!!
            }
        }
        if (basicAuth == null) {
            try {
                extractResponseParams(
                    rc.getFromEndpoint(
                        getParent()!!.name,
                        getParent()!!.parent!!.testId,
                        URL(addr),
                        params
                    )
                )
            } catch (e: MalformedURLException) { // TODO Auto-generated catch block
                log.error(e)
            } catch (e: IOException) { // TODO Auto-generated catch block
                log.error(e)
            }
        } else {
            try {
                extractResponseParams(
                    rc.getFromEndpointWithAuth(
                        getParent()!!.name,
                        getParent()!!.parent!!.testId,
                        URL(addr),
                        params,
                        knownParams[basicAuth!!.user],
                        knownParams[basicAuth!!.password]
                    )
                )
            } catch (e: MalformedURLException) { // TODO Auto-generated catch block
                log.error(e)
            } catch (e: IOException) { // TODO Auto-generated catch block
                log.error(e)
            }
        }
    }

    private fun handleGetWithBody() {
        val params = HashMap<String, String>()
        if (requestJSONObject != null) { //fill out template
        }
        var jsonParams: String? = ""
        try {
            jsonParams = om.writeValueAsString(params)
        } catch (e1: JsonProcessingException) { // TODO Auto-generated catch block
            e1.printStackTrace()
        }
        if (basicAuth == null) {
            try {
                extractResponseParams(
                    rc.getBodyFromEndpoint(
                        getParent()!!.name,
                        getParent()!!.parent!!.testId,
                        URL(addr),
                        jsonParams
                    )
                )
            } catch (e: MalformedURLException) { // TODO Auto-generated catch block
                log.error(e)
            } catch (e: IOException) { // TODO Auto-generated catch block
                log.error(e)
            }
        } else {
            try {
                extractResponseParams(
                    rc.getBodyFromEndpointWithAuth(
                        getParent()!!.name,
                        getParent()!!.parent!!.testId,
                        URL(addr),
                        jsonParams,
                        knownParams[basicAuth!!.user],
                        knownParams[basicAuth!!.password]
                    )
                )
            } catch (e: MalformedURLException) { // TODO Auto-generated catch block
                log.error(e)
            } catch (e: IOException) { // TODO Auto-generated catch block
                log.error(e)
            }
        }
    }

    override fun equals(o: Any?): Boolean {
        if (o === this) return true
        if (o !is Request) return false
        val other = o
        if (!other.canEqual(this as Any)) return false
        if (!super.equals(o)) return false
        val `this$verb`: Any? = verb
        val `other$verb`: Any? = other.verb
        if (if (`this$verb` == null) `other$verb` != null else `this$verb` != `other$verb`) return false
        val `this$addr`: Any? = addr
        val `other$addr`: Any? = other.addr
        if (if (`this$addr` == null) `other$addr` != null else `this$addr` != `other$addr`) return false
        if (!Arrays.deepEquals(requestParams, other.requestParams)) return false
        if (!Arrays.deepEquals(responseParams, other.responseParams)) return false
        val `this$requestJSONObject`: Any? = requestJSONObject
        val `other$requestJSONObject`: Any? = other.requestJSONObject
        if (if (`this$requestJSONObject` == null) `other$requestJSONObject` != null else `this$requestJSONObject` != `other$requestJSONObject`) return false
        if (!Arrays.deepEquals(responseJSONObject, other.responseJSONObject)) return false
        val `this$basicAuth`: Any? = basicAuth
        val `other$basicAuth`: Any? = other.basicAuth
        if (if (`this$basicAuth` == null) `other$basicAuth` != null else `this$basicAuth` != `other$basicAuth`) return false
        if (!Arrays.deepEquals(assertions, other.assertions)) return false
        val `this$implicitNotFailedAssertion`: Any? = implicitNotFailedAssertion
        val `other$implicitNotFailedAssertion`: Any? = other.implicitNotFailedAssertion
        return if (if (`this$implicitNotFailedAssertion` == null) `other$implicitNotFailedAssertion` != null else `this$implicitNotFailedAssertion` != `other$implicitNotFailedAssertion`) false else true
    }

    override fun canEqual(other: Any?): Boolean {
        return other is Request
    }

    override fun hashCode(): Int {
        val PRIME = 59
        var result = super.hashCode()
        val `$verb`: Any? = verb
        result = result * PRIME + (`$verb`?.hashCode() ?: 43)
        val `$addr`: Any? = addr
        result = result * PRIME + (`$addr`?.hashCode() ?: 43)
        result = result * PRIME + Arrays.deepHashCode(requestParams)
        result = result * PRIME + Arrays.deepHashCode(responseParams)
        val `$requestJSONObject`: Any? = requestJSONObject
        result = result * PRIME + (`$requestJSONObject`?.hashCode() ?: 43)
        result = result * PRIME + Arrays.deepHashCode(responseJSONObject)
        val `$basicAuth`: Any? = basicAuth
        result = result * PRIME + (`$basicAuth`?.hashCode() ?: 43)
        result = result * PRIME + Arrays.deepHashCode(assertions)
        val `$implicitNotFailedAssertion`: Any? = implicitNotFailedAssertion
        result =
            result * PRIME + (`$implicitNotFailedAssertion`?.hashCode() ?: 43)
        return result
    }

    class BasicAuth {
        var user: String? = null
        var password: String? = null

        constructor(user: String?, password: String?) {
            this.user = user
            this.password = password
        }

        constructor() {}

    }

    companion object {
        private val log =
            LogManager.getLogger(Request::class.java)
        @JsonIgnore
        private val rc = RestClient()
        @JsonIgnore
        private val om = ObjectMapper()
    }
}