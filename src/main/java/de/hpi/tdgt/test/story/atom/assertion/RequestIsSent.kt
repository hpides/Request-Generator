package de.hpi.tdgt.test.story.atom.assertion

import de.hpi.tdgt.requesthandling.RestResult
import org.apache.logging.log4j.LogManager

class RequestIsSent : Assertion() {
    override fun check(restResult: RestResult?, testid: Long) {
        if (restResult != null && restResult.errorCondition != null) {
            log.error("Failed request is sent assertion\"" + name + "\": " + restResult.errorCondition!!.message)
            AssertionStorage.instance.addFailure(
                name,
                restResult.errorCondition!!.javaClass.simpleName + ":" + restResult.errorCondition!!.message,
                testid
            )
        }
    }

    override fun equals(other: Any?): Boolean {
        if (other === this) return true
        if (other !is RequestIsSent) return false
        if (!other.canEqual(this as Any)) return false
        return if (!super.equals(other)) false else true
    }

    override fun canEqual(other: Any?): Boolean {
        return other is RequestIsSent
    }

    override fun hashCode(): Int {
        return super.hashCode()
    }

    companion object {
        private val log =
            LogManager.getLogger(RequestIsSent::class.java)
    }
}