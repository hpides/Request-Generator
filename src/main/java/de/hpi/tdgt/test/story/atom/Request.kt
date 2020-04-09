package de.hpi.tdgt.test.story.atom

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.ObjectMapper
import de.hpi.tdgt.requesthandling.RestClient
import de.hpi.tdgt.requesthandling.RestResult
import de.hpi.tdgt.test.story.UserStory
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
        set(value){
            field = value
            unescapedAddr = value
        }
    private var unescapedAddr: String? = null
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

    /**
     * If true, times for endpoints with variable expansion in the addresses are recorded as they are given (resulting in all of them showing one graph line).
     * Else, all of them will be displayed individually.
     * Enable for small-scope debuging, disable for large-scope tests.
     */
    var timeAggregation:Boolean = true

    /**
     * Contains an expression that might contain variable expansions to the left and names to send the generated values under to the rigth.
     */
    var sendHeaders: Map<String, String> = HashMap()

    /**
     * Contains names of headers for which the value shall be extracted under the given names from the response and which shall be saved under that name in the token.
     */
    var receiveHeaders: Array<String> = emptyArray()

    private fun getRecordName():String?{
        if(timeAggregation){
            return unescapedAddr
        }
        return addr
    }

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
        val request = de.hpi.tdgt.requesthandling.Request()
        request.method = verb
        request.recordName = getRecordName()
        request.url = URL(addr)
        if (requestJSONObject != null) {
            request.body = replaceWithKnownParams(requestJSONObject!!, true)
            request.isForm = false
        } else if(requestParams.isNotEmpty()){
            val params = HashMap<String,String>()
            for (key in requestParams) {
                // sometimes, a space sneaks in
                if(knownParams.get(key.trim()) != null) {
                    params[key.trim()] = knownParams[key.trim()]!!
                }
            }
            request.params = params
            request.isForm = true
        }
        if(basicAuth != null){
            request.username =  knownParams[basicAuth!!.user]
            request.password = knownParams[basicAuth!!.password]
        }
        for(header in this.sendHeaders){
            request.sendHeaders[header.value] = replaceWithKnownParams(header.key,false)?:""
        }
        request.story = getParent()?: UserStory()
        // in tests and in "pure" usage, parent might be nuull and we do not want to fail because of this
        request.testId = ((getParent()?:UserStory()).parent?.testId)?:0L
        request.receiveCookies = receiveCookies.keys.toTypedArray()
        request.sendCookies = prepareCookies()
        extractResponseParams(rc.exchangeWithEndpoint(request))
    }

    /**
     * During cloning, replacement will not work. This Flag disables it so the user does not get confised with warning messages.
     */
    private var cloning = false;

    public override fun performClone(): Atom {
        cloning = true;
        val ret = Request()
        ret.cloning = true
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
        ret.timeAggregation = timeAggregation
        ret.receiveHeaders = receiveHeaders
        ret.sendHeaders = sendHeaders
        cloning = false;
        ret.cloning = false
        return ret
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
            try {
                if (result.toJson()!!.isObject) {
                    val json = String(result.response, StandardCharsets.UTF_8)
                    val map: Map<*, *> =
                            om.readValue(
                                    json,
                                    MutableMap::class.java
                            )
                    val strMap = toStringMap(map)
                    //only keep params the user actually wanted
                    for(param in responseJSONObject){
                        knownParams.put(param, strMap[param]?:"")
                    }
                } else {
                    log.info("I can not handle Arrays.")
                    log.info(result)
                }
            } catch (e: JsonParseException){
                reportFailureToUser("Request $name can parse response JSON",e.message)
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
                knownParams[receiveCookies[cookie]!!] = result.receivedCookies[cookie]!!
            }
        }

        //in some tests, this might not exist
        if (getParent() != null && getParent()!!.parent != null) { //check assertions after request
            for (assertion in assertions) {
                assertion!!.check(result, getParent()!!.parent!!.testId, this)
            }
            implicitNotFailedAssertion!!.check(result, getParent()!!.parent!!.testId, this)
        } else {
            log.error("Can not check assertions because I do not have a parent or grandparent: $name")
        }

        if(result?.headers != null) {
            for (header in this.receiveHeaders) {
                this.knownParams[header] = result.headers!![header]
            }
        }
    }

    public fun replaceWithKnownParams(toReplace: String, enquoteInsertedValue:Boolean,sanitizeXPATH:Boolean = false, sanitizeJSONPATH: Boolean = false): String? {
        var current = toReplace
        for ((key, value) in knownParams) {
            var useValue=value
            if(sanitizeXPATH){
                useValue = sanitizeXPATH(value)
            }
            //sanitizeXPATH takes care of quotes
            if(enquoteInsertedValue && !sanitizeXPATH && !sanitizeJSONPATH) {
                // quotes in JSON need to be replaced
                useValue = useValue.replace("\"","\\\"")
                current = current.replace("$$key", '\"' + useValue + '\"')
            }
            else if(sanitizeJSONPATH){
                //inside a JSONPATH regex, forward slashes need to be escaped since they denominate regexes. Else they should not be replaced because it might alter the meaning.
                var isInRegex = false
                val keyStart = current.indexOf(key)
                if(keyStart > 0){
                    var index = 0;
                    while(index < keyStart){
                        if(current[index] == '/') isInRegex = !isInRegex
                        index++
                    }
                }
                if(isInRegex) {
                    useValue = useValue.replace("/","\\/")
                }
                //symbols that always need replacement
                useValue = useValue.replace("(","\\(").replace(")","\\)").replace("'","\\'").replace("\"","\\\"")

                current = current.replace("$$key", useValue)
            }
            else{
                current = current.replace("$$key", useValue)
            }
        }
        //should show a warning
        //need to consider surrounding characters in both direction, else it does not match...
        //method might be called during cloning (usage in setters). In this case, we do not want it to report failed assertions
        if (Pattern.matches(".*"+"\\"+"\$"+"[a-zA-Z0-9]+.*", current) && !cloning) {
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
