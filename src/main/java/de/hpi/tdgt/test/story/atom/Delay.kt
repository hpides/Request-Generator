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

package de.hpi.tdgt.test.story.atom

import de.hpi.tdgt.util.PropertiesReader
import kotlinx.coroutines.delay
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

class Delay : Atom() {
    var delayMs:String? = "0"
    override suspend fun perform() {
        val evaluatedDelay = replaceWithKnownParams(delayMs?:"0",false)
        val actualDelay = evaluatedDelay?.toLongOrNull()
        try {
            if (actualDelay != null && actualDelay > 0) {
                if(PropertiesReader.AsyncIO()) {
                    delay(actualDelay)
                }else {
                    Thread.sleep(actualDelay)
                }
            }
            else if(actualDelay == null){
                reportFailureToUser("Delay $name could not delay: Expanded expression is not a Long!",evaluatedDelay)
            }
        } catch (e: InterruptedException) {
            log.error(e)
        }
    }

    public override fun performClone(): Atom {
        val ret = Delay()
        ret.delayMs = delayMs
        return ret
    }

    override fun equals(other: Any?): Boolean {
        if (other === this) return true
        if (other !is Delay) return false
        val otherObject = other
        if (!otherObject.canEqual(this as Any)) return false
        if (!super.equals(otherObject)) return false
        return if (delayMs != otherObject.delayMs) false else true
    }

    override fun canEqual(other: Any?): Boolean {
        return other is Delay
    }

    override fun hashCode(): Int {
        val PRIME = 59
        var result = super.hashCode()
        result = result * PRIME + delayMs.hashCode()
        return result
    }

    override val log: Logger
        get() = Delay.log

    companion object {
        private val log = LogManager.getLogger(Delay::class.java)
    }
}