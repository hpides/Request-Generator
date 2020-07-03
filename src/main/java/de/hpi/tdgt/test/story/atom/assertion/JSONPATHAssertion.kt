/*
 * WALT - A realistic load generator for web applications.
 *
 * Copyright 2020 Eric Ackermann <eric.ackermann@student.hpi.de>, Hendrik Bomhardt
 * <hendrik.bomhardt@student.hpi.de>, Benito Buchheim
 * <benito.buchheim@student.hpi.de>, Juergen Schlossbauer
 * <juergen.schlossbauer@student.hpi.de>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
    var returnResponse=false
    override fun check(restResult: RestResult?, testid: Long, parent: Request) {
        if(restResult!=null) {
            val response = String(restResult.response)
            val usePath = parent.replaceWithKnownParams(JSONPATH?:"",false, sanitizeJSONPATH = true)
                try {
                    val ctx: ReadContext = JsonPath.parse(response)
                    val responses : JSONArray = ctx.read(usePath)
                    if(responses.size == 0){
                        if(!returnResponse) {
                            log.error("Failed jsonpath assertion\"$name\": expected \"$usePath\" to find something but nothing was returned")
                            AssertionStorage.instance.addFailure(
                                name,
                                "jsonpath \"${usePath}\" returned empty result",
                                testid
                            )
                        } else{
                            log.error("Failed jsonpath assertion\"$name\": response was $response")
                            AssertionStorage.instance.addFailure(
                                name,
                                response,
                                testid
                            )
                        }
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