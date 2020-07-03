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

import lombok.EqualsAndHashCode
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

@EqualsAndHashCode(callSuper = false)
class Start : Atom() {
    @Throws(InterruptedException::class)
    override suspend fun perform() {
        //Noop, just supposed to start the following atoms
    }

    override fun performClone(): Atom {
        //stateless
        return Start()
    }

    override val log: Logger
        get() = Start.log

    companion object {
        val log = LogManager.getLogger(Start::class.java)
    }
}