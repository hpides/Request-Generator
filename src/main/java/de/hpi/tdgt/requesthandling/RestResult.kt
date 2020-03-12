package de.hpi.tdgt.requesthandling

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import io.netty.handler.codec.http.HttpHeaders
import java.io.IOException
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

class RestResult {
    //parameters for httpurlconnection
    var startTime: Long = 0
    var endTime: Long = 0
    var response: ByteArray = ByteArray(0)
    var contentType: String? = null
    var headers: HttpHeaders? = null
    var returnCode = 0
    var errorCondition: Exception? = null
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
        private get() {
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