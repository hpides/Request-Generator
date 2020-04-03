package de.hpi.tdgt.test.story.atom.assertion

import com.jayway.jsonpath.JsonPath
import com.jayway.jsonpath.ReadContext
import de.hpi.tdgt.requesthandling.RestResult
import de.hpi.tdgt.test.story.atom.Request
import net.minidev.json.JSONArray
import org.apache.logging.log4j.LogManager


class JSONPATHAssertion : Assertion() {
    var JSONPATH: String? = null
    set(value){
        //might confuse later logic if trailing / starting whitespaces are found
        field = value?.trim()
    }
    override fun check(restResult: RestResult?, testid: Long, parent: Request) {
        if(restResult!=null) {
            val response = String(restResult.response)
            val usePath = parent.replaceWithKnownParams(JSONPATH?:"",false, sanitizeJSONPATH = true)
                try {
                    val ctx: ReadContext = JsonPath.parse(response)
                    val responses : JSONArray = ctx.read(usePath)
                    if(responses.size == 0){
                        log.error("Failed jsonpath assertion\"$name\": expected \"$usePath\" to find something but nothing was returned")
                        AssertionStorage.instance.addFailure(name, "jsonpath \"${usePath}\" returned empty result", testid)
                    }
                } catch (e: Exception) {
                    log.error("Failed jsonpath assertion\"$name\": jsonpath \"$usePath\" is invalid :${e.message}")
                    AssertionStorage.instance.addFailure(name, "jsonpath \"${usePath}\" is invalid :${e.message}", testid)
                }
        }
    }
    companion object {
        private val log =
                LogManager.getLogger(JSONPATHAssertion::class.java)
    }

}