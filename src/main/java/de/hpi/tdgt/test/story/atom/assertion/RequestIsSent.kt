package de.hpi.tdgt.test.story.atom.assertion

import de.hpi.tdgt.stats.Endpoint
import de.hpi.tdgt.requesthandling.RestResult
import de.hpi.tdgt.test.story.atom.RequestAtom
import de.hpi.tdgt.test.time_measurement.TimeStorage
import org.apache.logging.log4j.LogManager

class RequestIsSent : Assertion() {
    override suspend fun check(endpoint: Endpoint, restResult: RestResult, parent: RequestAtom) {
        var error = restResult.errorCondition;
        if (error != null) {
            log.error("Failed request is sent assertion\"" + name + "\": " + error.message)
            TimeStorage.instance.addError(endpoint, error.toString());
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