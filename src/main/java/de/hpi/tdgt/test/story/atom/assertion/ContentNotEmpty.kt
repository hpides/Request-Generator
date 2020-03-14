package de.hpi.tdgt.test.story.atom.assertion

import de.hpi.tdgt.requesthandling.RestResult
import org.apache.logging.log4j.LogManager

class ContentNotEmpty : Assertion() {
    override fun check(restResult: RestResult?, testid: Long) {
        if (restResult != null && restResult.response.isEmpty()) {
            log.error("Failed content not empty assertion\"$name\": response was empty!")
            AssertionStorage.instance.addFailure(name, "", testid)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (other === this) return true
        if (other !is ContentNotEmpty) return false
        if (!other.canEqual(this as Any)) return false
        return if (!super.equals(other)) false else true
    }

    override fun canEqual(other: Any?): Boolean {
        return other is ContentNotEmpty
    }


    companion object {
        private val log = LogManager.getLogger(
            ContentNotEmpty::class.java
        )
    }
}