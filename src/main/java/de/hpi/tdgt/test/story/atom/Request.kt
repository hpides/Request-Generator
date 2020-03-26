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
import org.htmlcleaner.CleanerProperties
import org.htmlcleaner.DomSerializer
import org.htmlcleaner.HtmlCleaner
import java.io.IOException
import java.net.MalformedURLException
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.regex.Pattern
import javax.xml.xpath.XPath
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathFactory
import kotlin.collections.HashMap


class Request : Atom() {
    var verb: String? = null
    var addr: String? = null
        get() {
            if(field != null) {
                return replaceWithKnownParams(field!!,false)
            }
            return field
        }
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
    /**
     * Contains names of cookies to extract on the left and names to put their respective values under in the token knownParams in the right.
     */
    var receiveCookies: Map<String, String> = HashMap()

    /**
     * Contains names of cookies to extract from the knownParams map on the left and names to put them in HTTP Cookie Header on the right
     */
    var sendCookies: Map<String, String> = HashMap()

    /**
     * Left side is a name of a hidden input that should be looked for. Right is a key under which it should be saved in the knownParams list
     */
    var tokenNames: Map<String, String> = HashMap()

    /**
     * Left side is a valid XPATH expression, right side is the name under which the result shall be placed in a token
     */
    var xpaths: Map<String, String> = HashMap()

    var basicAuth: BasicAuth? = null
    var assertions = arrayOfNulls<Assertion>(0)
    var implicitNotFailedAssertion: Assertion? = null



    fun parseXPaths(returnedPage: String, xpaths:Map<String,String>){
        //turns HTML into valid xml
        val tagNode = HtmlCleaner().clean(returnedPage)
        val doc = DomSerializer(
                CleanerProperties()
        ).createDOM(tagNode)
        val xpath: XPath = XPathFactory.newInstance().newXPath()
        for(entry in xpaths) {
            var str:String;
            val expression = replaceWithKnownParams(entry.key, enquoteInsertedValue = true, sanitizeXPATH = true)
            log.info(expression)
            try {
                str = xpath.evaluate(
                        expression,
                        doc, XPathConstants.STRING
                ) as String
            } catch (e:Exception){
                reportFailureToUser("XPATH failed: \"${expression}\"", e.message)
                continue
            }
            knownParams.put(entry.value, str)
        }
    }

    /**
     * Extracts all hidden inputs whose name is one of the keys in tokenNames and stores their values in knownParams
     */
    fun extractCSRFTokens(returnedPage: String){
        if(tokenNames.isEmpty()&& xpaths.isEmpty()){
            return
        }
        val allXPaths = HashMap<String, String>()
        allXPaths.putAll(xpaths)
        for(entry in tokenNames.entries) {

            val expression = "//input[@type = 'hidden'][@name = '${entry.key}']/@value"
            allXPaths.put(expression, entry.value)
        }
        parseXPaths(returnedPage, allXPaths)
    }

    private fun prepareCookies():Map<String, String>{
        val ret = HashMap<String, String>()
        for(cookie in this.sendCookies.entries){
            val value = this.knownParams.get(cookie.key);
            if(value == null){
                log.warn("Cookie "+cookie.key+" not known!")
                continue
            }
            ret.put(cookie.value, value)
        }
        return ret
    }
    
    @Throws(InterruptedException::class)
    override suspend fun perform() {
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

    /**
     * During cloning, replacement will not work. This Flag disables it so the user does not get confised with warning messages.
     */
    private var cloning = false;

    public override fun performClone(): Atom {
        cloning = true;
        val ret = Request()
        ret.addr = addr
        ret.verb = verb
        ret.responseJSONObject = responseJSONObject
        ret.requestParams = requestParams
        ret.basicAuth = basicAuth
        ret.requestJSONObject = requestJSONObject
        ret.responseJSONObject = responseJSONObject
        ret.responseParams = responseParams
        ret.sendCookies = sendCookies
        ret.receiveCookies = receiveCookies
        ret.tokenNames = tokenNames
        ret.xpaths = xpaths
        //also stateless
        ret.assertions = assertions
        cloning = false;
        return ret
    }

    private suspend fun handlePost() {
        if (requestJSONObject != null) {
            handlePostWithBody()
        } else {
            handlePostWithForm()
        }
    }

    private suspend fun handlePostWithForm() {
        val params = HashMap<String, String>()
        for (key in requestParams) {
            log.info("Key "+key+" known "+knownParams.get(key))
            // sometimes, a space sneaks in
            if(knownParams.get(key.trim()) != null) {
                params[key.trim()] = knownParams[key.trim()]!!
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
                            receiveCookies.keys.toTypedArray(),
                            prepareCookies(),
                            params
                        )
                    )
                } else {
                    extractResponseParams(
                        rc.postFormToEndpoint(
                            "unknown",
                            0,
                            URL(addr),
                            receiveCookies.keys.toTypedArray(),
                            prepareCookies(),
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
                            receiveCookies.keys.toTypedArray(),
                            prepareCookies(),
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
    private suspend fun extractResponseParams(result: RestResult?) {
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
            log.info("Not JSON! Response is ignored.")
            log.info(result)
        }

        if(result != null && result.isHtml){
            extractCSRFTokens(String(result.response))
        }

        //given cookie map is indirection between cookie name and name to put it into because of namespacing
        for(cookie in receiveCookies.keys){
            if (result != null && result.receivedCookies.get(cookie) != null) {
                knownParams.put(receiveCookies.get(cookie)!!, result.receivedCookies.get(cookie)!!)
            }
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

    private fun replaceWithKnownParams(toReplace: String, enquoteInsertedValue:Boolean,sanitizeXPATH:Boolean = false): String? {
        var current = toReplace
        for ((key, value) in knownParams) {
            var useValue=value
            if(sanitizeXPATH){
                useValue = sanitizeXPATH(value)
            }
            //sanitizeXPATH takes care of quotes
        if(enquoteInsertedValue && !sanitizeXPATH) {
                current = current.replace("$$key", '\"' + useValue + '\"')
            }
            else{
                current = current.replace("$$key", useValue)
            }
        }
        //should show a warning
        //need to consider surrounding characters in both direction, else it does not match...
        //method might be called during cloning (usage in setters). In this case, we do not want it to report failed assertions
        if (Pattern.matches(".*"+"\\"+"\$"+"[a-zA-Z0-9]*.*", current) && !cloning) {
            val p = Pattern.compile("\\$[a-zA-Z0-9]*")
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
                //we want to show the "pure" variable names
                builder.append(' ').append(unmatched.replace("\$",""))
            }
            log.warn("Request $name: Could not replace variable(s) $builder")
            reportFailureToUser("Request $name: Could not replace variable(s) $builder", current)
        }
        return current
    }

    private suspend fun handlePostWithBody() {
        var jsonParams: String? = ""
        if (requestJSONObject != null) {
            jsonParams = replaceWithKnownParams(requestJSONObject!!, true)
        }
        if (basicAuth == null) {
            try {
                extractResponseParams(
                    rc.postBodyToEndpoint(
                        getParent()!!.name,
                        getParent()!!.parent!!.testId,
                        URL(addr),
                            receiveCookies.keys.toTypedArray(),
                            prepareCookies(),
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
                            receiveCookies.keys.toTypedArray(),
                            prepareCookies(),
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

    private suspend fun handlePut() {
        if (requestJSONObject != null) {
            handlePutWithBody()
        } else {
            handlePutWithForm()
        }
    }

    private suspend fun handlePutWithForm() {
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
                            receiveCookies.keys.toTypedArray(),
                            prepareCookies(),
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
                            receiveCookies.keys.toTypedArray(),
                            prepareCookies(),
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

    private suspend fun handlePutWithBody() {
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
                            receiveCookies.keys.toTypedArray(),
                            prepareCookies(),
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
                            receiveCookies.keys.toTypedArray(),
                            prepareCookies(),
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

    private suspend fun handleDelete() {
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
                            receiveCookies.keys.toTypedArray(),
                            prepareCookies(),
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
                            receiveCookies.keys.toTypedArray(),
                            prepareCookies(),
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

    private suspend fun handleGet() {
        if (requestJSONObject != null) {
            handleGetWithBody()
        } else {
            handleGetWithForm()
        }
    }

    private suspend fun handleGetWithForm() {
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
                        if(getParent() != null){getParent()!!.name}else{""}, 
                        if(getParent() != null && getParent()!!.parent != null){getParent()!!.parent!!.testId}else{0},
                        URL(addr),
                            receiveCookies.keys.toTypedArray(),
                            prepareCookies(),
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
                            receiveCookies.keys.toTypedArray(),
                            prepareCookies(),
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

    private suspend fun handleGetWithBody() {
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
                        if(getParent() != null){getParent()!!.name}else{""}, 
                        if(getParent() != null && getParent()!!.parent != null){getParent()!!.parent!!.testId}else{0},
                        URL(addr),
                            receiveCookies.keys.toTypedArray(),
                            prepareCookies(),
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
                            receiveCookies.keys.toTypedArray(),
                            prepareCookies(),
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

    override fun equals(other: Any?): Boolean {
        if (other === this) return true
        if (other !is Request) return false
        if (!other.canEqual(this as Any)) return false
        if (!super.equals(other)) return false
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
        @JvmStatic
        public fun sanitizeXPATH(expression:String):String{
            //HTMLCleaner does the same with the HTML input
            val expressionToUse = expression.replace("\"","&quot;")
            val builder = StringBuilder("concat(\"")
            //still need to replace single quotes
            for(character in expressionToUse.toCharArray()){
                if (character == '\''){
                    builder.append("\",\"'\",\"")
                }
                else {
                    builder.append(character)
                }
            }
            builder.append("\",\"\")")
            return builder.toString()
        }
    }
}