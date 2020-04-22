package de.hpi.tdgt.test.story.atom

import de.hpi.tdgt.util.PropertiesReader
import kotlinx.coroutines.delay
import org.apache.logging.log4j.LogManager

class DelayAtom : Atom() {
    var delayMs = 0
    override suspend fun perform() {
        try {
            if (delayMs > 0) {
                if(PropertiesReader.AsyncIO()) {
                    delay(delayMs.toLong())
                }else {
                    Thread.sleep(delayMs.toLong())
                }
            }
        } catch (e: InterruptedException) {
            log.error(e)
        }
    }

    public override fun performClone(): Atom {
        val ret = DelayAtom()
        ret.delayMs = delayMs
        return ret
    }

    override fun equals(other: Any?): Boolean {
        if (other === this) return true
        if (other !is DelayAtom) return false
        val otherObject = other
        if (!otherObject.canEqual(this as Any)) return false
        if (!super.equals(otherObject)) return false
        return if (delayMs != otherObject.delayMs) false else true
    }

    override fun canEqual(other: Any?): Boolean {
        return other is DelayAtom
    }

    override fun hashCode(): Int {
        val PRIME = 59
        var result = super.hashCode()
        result = result * PRIME + delayMs
        return result
    }

    companion object {
        private val log = LogManager.getLogger(DelayAtom::class.java)
    }
}