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

package de.hpi.tdgt.test.story.atom.assertion

import de.hpi.tdgt.requesthandling.RestResult
import de.hpi.tdgt.test.story.atom.Request
import org.apache.logging.log4j.LogManager

class ContentNotEmpty : Assertion() {
    override fun check(restResult: RestResult?, testid: Long, parent: Request) {
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