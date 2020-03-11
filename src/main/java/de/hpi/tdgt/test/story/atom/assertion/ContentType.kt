package de.hpi.tdgt.test.story.atom.assertion

import de.hpi.tdgt.requesthandling.RestResult
import org.apache.logging.log4j.LogManager

class ContentType : Assertion() {
    var contentType: String? = null
    override fun check(restResult: RestResult?, testid: Long) {
        if (restResult != null && contentType != restResult.contentType) {
            log.error("Failed content type assertion\"" + name + "\": expected \"" + contentType + "\" but is actually \"" + restResult.contentType + "\"!")
            AssertionStorage.instance.addFailure(name, restResult.contentType!!, testid)
        }
    }

    override fun equals(o: Any?): Boolean {
        if (o === this) return true
        if (o !is ContentType) return false
        val other =
            o
        if (!other.canEqual(this as Any)) return false
        if (!super.equals(o)) return false
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