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

package de.hpi.tdgt.requesthandling

import de.hpi.tdgt.test.story.UserStory
import java.net.URL
import java.util.HashMap

/**
 * Represents a HTTP Request.
 */
class Request {
    var url: URL? = null
    /**
     * Params for GET URL / POST / PUT Form-Encoded data.
     */
    var params: Map<String, String>? = null
    /**
     * Name for this request in time storage
     */
    var recordName:String? = null
    var method: String? = null
    var isFollowsRedirects = true
    var connectTimeout = -1
    var responseTimeout = -1
    var isSendKeepAlive = false
    var retries = 0
    /**
     * Body for POST/PUT.
     */
    var body: String? = null
    /**
     * Set to true if using PUT / POST you want to send URL-Encoded parameters. Else set to false.
     */
    var isForm = false
    var username: String? = null
    var password: String? = null
    /**
     * To account time to a story.
     */
    var story: UserStory? = null
    /**
     * To match MQTT messages to a test.
     */
    var testId: Long = 0

    var receiveCookies: Array<String> = emptyArray()


    /**
     * Contains cookie name (left) and value(right)
     */
    var sendCookies: Map<String, String> = HashMap()

    /**
     * Contains names of headers and values to send to the target system
     */
    val sendHeaders: MutableMap<String, String> =  HashMap()

    constructor(
        url: URL?,
        params: Map<String, String>?,
        method: String?,
        followsRedirects: Boolean,
        connectTimeout: Int,
        responseTimeout: Int,
        sendKeepAlive: Boolean,
        retries: Int,
        body: String?,
        form: Boolean,
        username: String?,
        password: String?,
        story: UserStory?,
        testId: Long
    ) {
        this.url = url
        this.params = params
        this.method = method
        isFollowsRedirects = followsRedirects
        this.connectTimeout = connectTimeout
        this.responseTimeout = responseTimeout
        isSendKeepAlive = sendKeepAlive
        this.retries = retries
        this.body = body
        isForm = form
        this.username = username
        this.password = password
        this.story = story
        this.testId = testId
    }

    constructor() {}

}