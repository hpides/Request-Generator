package de.hpi.tdgt.test.story.atom.assertion

import com.jayway.jsonpath.JsonPath
import com.jayway.jsonpath.ReadContext
import de.hpi.tdgt.Stats.Endpoint
import de.hpi.tdgt.requesthandling.RestResult
import de.hpi.tdgt.test.story.atom.RequestAtom
import de.hpi.tdgt.test.time_measurement.TimeStorage
import net.minidev.json.JSONArray
import org.apache.logging.log4j.LogManager


class JSONPATHAssertion : Assertion() {
    var JSONPATH: String? = null
        set(value) {
            //might confuse later logic if trailing / starting whitespaces are found
            field = value?.trim()
        }
    var returnResponse = false
    override suspend fun check(endpoint: Endpoint, restResult: RestResult, parent: RequestAtom) {
        val response = String(restResult.response)
        val usePath = parent.replaceWithKnownParams(JSONPATH ?: "", false, sanitizeJSONPATH = true)
        try {
            val ctx: ReadContext = JsonPath.parse(response)
            val responses: JSONArray = ctx.read(usePath)
            if (responses.size == 0) {
                if (!returnResponse) {
                    log.error("Failed jsonpath assertion\"$name\": expected \"$usePath\" to find something but nothing was returned")
                    TimeStorage.instance.addError(endpoint, "JSONPath result is empty");
                } else {
                    log.error("Failed jsonpath assertion\"$name\": response was $response")
                    TimeStorage.instance.addError(endpoint, "JSONPath wrong result");
                }
            }
        } catch (e: Exception) {
            log.error("Failed jsonpath assertion\"$name\": jsonpath \"$usePath\" is invalid :${e.message}")
            TimeStorage.instance.addError(endpoint, e.toString());
        }
    }

    companion object {
        private val log =
                LogManager.getLogger(JSONPATHAssertion::class.java)
    }

}