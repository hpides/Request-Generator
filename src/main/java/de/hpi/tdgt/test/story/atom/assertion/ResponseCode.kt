package de.hpi.tdgt.test.story.atom.assertion

import de.hpi.tdgt.Stats.Endpoint
import de.hpi.tdgt.requesthandling.RestResult
import de.hpi.tdgt.test.story.atom.RequestAtom
import de.hpi.tdgt.test.time_measurement.TimeStorage
import org.apache.logging.log4j.LogManager

class ResponseCode : Assertion() {
    var responseCode = 0
    override suspend fun check(endpoint: Endpoint, restResult: RestResult, parent: RequestAtom) {
        if (responseCode != restResult.returnCode) {
            log.error("Failed response code assertion\"" + name + "\": expected \"" + responseCode + "\" but is actually \"" + restResult.returnCode + "\"!")
            TimeStorage.instance.addError(endpoint, "Wrong Response Code");
        }
    }

    override fun equals(other: Any?): Boolean {
        if (other === this) return true
        if (other !is ResponseCode) return false
        if (!other.canEqual(this as Any)) return false
        if (!super.equals(other)) return false
        return if (responseCode != other.responseCode) false else true
    }

    override fun canEqual(other: Any?): Boolean {
        return other is ResponseCode
    }

    override fun hashCode(): Int {
        val PRIME = 59
        var result = super.hashCode()
        result = result * PRIME + responseCode
        return result
    }

    companion object {
        private val log =
            LogManager.getLogger(ResponseCode::class.java)
    }
}