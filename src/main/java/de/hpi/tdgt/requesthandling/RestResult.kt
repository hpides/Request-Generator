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

package de.hpi.tdgt.requesthandling

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import io.netty.handler.codec.http.HttpHeaders
import java.io.IOException
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.util.*
import kotlin.collections.HashMap

class RestResult {
    //parameters for httpurlconnection
    var startTime: Long = 0
    var endTime: Long = 0
    var response: ByteArray = ByteArray(0)
    var contentType: String? = null
    var headers: HttpHeaders? = null
    var returnCode = 0
    var errorCondition: Exception? = null

    var receivedCookies:MutableMap<String,String> = HashMap()

    //check content encoding
    val isPlainText: Boolean
        get() = contentType != null && contentType!!.replace(
            "\\s+".toRegex(),
            ""
        ).toLowerCase().startsWith(HttpConstants.CONTENT_TYPE_TEXT_PLAIN)

    val isJSON: Boolean
        get() = contentType != null && contentType!!.replace(
            "\\s+".toRegex(),
            ""
        ).toLowerCase().startsWith(HttpConstants.CONTENT_TYPE_APPLICATION_JSON)

    val isHtml: Boolean
        get() = contentType != null && contentType!!.replace(
            "\\s+".toRegex(),
            ""
        ).toLowerCase().contains("html")

    //use directly or deserialize
    override fun toString(): String {
        return if (isPlainText || isJSON) {
            String(response, charset)
        } else {
            ""
        }
    }

    //parse contenttype header
    private val charset: Charset
        get() {
            val contentTypeHeader =
                contentType!!.toLowerCase().split(";charset=".toRegex()).toTypedArray()
            return if (contentTypeHeader.size == 2) {
                Charset.forName(contentTypeHeader[1])
            } else StandardCharsets.US_ASCII
        }

    val mapper = ObjectMapper()
    //return JsonNode, because we do not know if it is array or object
    @Throws(IOException::class)
    fun toJson(): JsonNode? {
        return if (!isJSON) {
            null
        } else mapper.readTree(response)
    }

    //calculate durations
    fun durationNanos(): Long {
        return endTime - startTime
    }

    fun durationMillis(): Long {
        return durationNanos() / 1000000
    }

}