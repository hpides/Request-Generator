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

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import de.hpi.tdgt.requesthandling.RestResult
import de.hpi.tdgt.test.story.atom.Request
import org.apache.logging.log4j.LogManager

//tell Jackson to use subclasses by type attribute
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type"
) //tell Jackson which subtypes are allowed
@JsonSubTypes(
    JsonSubTypes.Type(value = ContentNotEmpty::class, name = "CONTENT_NOT_EMPTY"),
    JsonSubTypes.Type(value = ContentType::class, name = "CONTENT_TYPE"),
    JsonSubTypes.Type(value = ResponseCode::class, name = "RESPONSE_CODE"),
    JsonSubTypes.Type(value = XPATHAssertion::class, name = "XPATH"),
    JsonSubTypes.Type(value = JSONPATHAssertion::class, name = "JSONPATH")
)
abstract class Assertion {
    var name: String = ""

    constructor(name: String) {
        this.name = name
    }

    constructor()

    abstract fun check(restResult: RestResult?, testid: Long, parent: Request)

    override fun equals(other: Any?): Boolean {
        if (other === this) return true
        if (other !is Assertion) return false
        val otherObject = other
        if (!otherObject.canEqual(this as Any)) return false
        val `this$name`: Any? = name
        val `other$name`: Any? = otherObject.name
        return if (if (`this$name` == null) `other$name` != null else `this$name` != `other$name`) false else true
    }

    protected open fun canEqual(other: Any?): Boolean {
        return other is Assertion
    }

    override fun hashCode(): Int {
        val PRIME = 59
        var result = 1
        val `$name`: Any? = name
        result = result * PRIME + (`$name`?.hashCode() ?: 43)
        return result
    }

    companion object {
        private val log =
            LogManager.getLogger(Assertion::class.java)
        @JvmStatic
        public var oneHasFailed : Boolean = false
    }
}