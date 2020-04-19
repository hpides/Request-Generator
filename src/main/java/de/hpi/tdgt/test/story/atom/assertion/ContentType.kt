package de.hpi.tdgt.test.story.atom.assertion

import de.hpi.tdgt.Stats.Endpoint
import de.hpi.tdgt.requesthandling.RestResult
import de.hpi.tdgt.test.story.atom.RequestAtom
import de.hpi.tdgt.test.time_measurement.TimeStorage
import org.apache.logging.log4j.LogManager

class ContentType : Assertion() {
    var contentType: String? = null
    override suspend fun check(endpoint: Endpoint, restResult: RestResult, parent: RequestAtom) {
        if (contentType != restResult.contentType) {
            log.error("Failed content type assertion\"" + name + "\": expected \"" + contentType + "\" but is actually \"" + restResult.contentType + "\"!")
            TimeStorage.instance.addError(endpoint, "Type check failed");
        }
    }

    override fun equals(other: Any?): Boolean {
        if (other === this) return true
        if (other !is ContentType) return false
        if (!other.canEqual(this as Any)) return false
        if (!super.equals(other)) return false
        val `this$contentType`: Any? = contentType
        val `other$contentType`: Any? = other.contentType
        return if (if (`this$contentType` == null) `other$contentType` != null else `this$contentType` != `other$contentType`) false else true
    }

    override fun canEqual(other: Any?): Boolean {
        return other is ContentType
    }

    override fun hashCode(): Int {
        val PRIME = 59
        var result = super.hashCode()
        val `$contentType`: Any? = contentType
        result = result * PRIME + (`$contentType`?.hashCode() ?: 43)
        return result
    }

    companion object {
        private val log =
            LogManager.getLogger(ContentType::class.java)
    }
}