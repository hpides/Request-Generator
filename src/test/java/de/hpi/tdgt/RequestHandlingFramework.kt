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

package de.hpi.tdgt

import com.sun.net.httpserver.HttpServer
import de.hpi.tdgt.test.story.atom.DataGeneration.Companion.reset
import de.hpi.tdgt.test.story.atom.assertion.AssertionStorage
import de.hpi.tdgt.test.time_measurement.TimeStorage
import org.apache.commons.io.IOUtils
import org.apache.logging.log4j.LogManager
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.InetSocketAddress
import java.util.*
import java.util.function.Consumer

open class RequestHandlingFramework {
    @JvmField
    protected val getHandler = HttpHandlers.GetHandler()
    protected val getWithBodyHandler = HttpHandlers.GetWithBodyHandler()
    @JvmField
    protected val jsonObjectGetHandler = HttpHandlers.JSONObjectGetHandler()
    protected val jsonArrayGetHandler = HttpHandlers.JSONArrayGetHandler()
    protected val postHandler = HttpHandlers.PostHandler()
    @JvmField
    protected val postBodyHandler = HttpHandlers.PostBodyHandler()
    protected val putBodyHandler = HttpHandlers.PostBodyHandler()
    @JvmField
    protected val authHandler = HttpHandlers.AuthHandler()
    protected val emptyResponseHandler = HttpHandlers.EmptyResponseHandler()
    protected val cookiehandler = HttpHandlers.CookieResponseHandler()
    protected val htmlHandler = HttpHandlers.HTMLHandler()
    protected val headerHandler = HttpHandlers.CustomHeaderHandler()
    protected lateinit var server: HttpServer
    @JvmField
    protected var handlers: MutableList<HttpHandlers.HttpHandlerBase> = ArrayList()
    //Based on https://www.codeproject.com/tips/1040097/create-a-simple-web-server-in-java-http-server
    @BeforeEach
    @Throws(IOException::class)
    fun launchTestServer() {
        val port = 9000
        server = HttpServer.create(InetSocketAddress(port), 0)
        log.info("server started at $port")
        server.createContext("/", getHandler)
        server.createContext("/getWithBody", getWithBodyHandler)
        server.createContext("/jsonObject", jsonObjectGetHandler)
        server.createContext("/jsonArray", jsonArrayGetHandler)
        server.createContext("/echoPost", postHandler)
        server.createContext("/postWithBody", postBodyHandler)
        server.createContext("/putWithBody", putBodyHandler)
        server.createContext("/auth", authHandler)
        server.createContext("/empty", emptyResponseHandler)
        server.createContext("/cookie", cookiehandler)
        server.createContext("/html", htmlHandler)
        server.createContext("/headers", headerHandler)
        server.setExecutor(null)
        server.start()
        handlers.add(getHandler)
        handlers.add(getWithBodyHandler)
        handlers.add(jsonObjectGetHandler)
        handlers.add(jsonArrayGetHandler)
        handlers.add(postHandler)
        handlers.add(postBodyHandler)
        handlers.add(putBodyHandler)
        handlers.add(authHandler)
        handlers.add(emptyResponseHandler)
        handlers.add(cookiehandler)
        handlers.add(htmlHandler)
        handlers.add(headerHandler)
        val values = File("values.csv")
        values.deleteOnExit()
        val os = FileOutputStream(values)
        IOUtils.copy(Utils().valuesCSV, os)
        os.close()
        //tests want predictable behaviour in regards to when an entry is stored
        AssertionStorage.instance.isStoreEntriesAsynch = false
    }

    @AfterEach
    fun removeSideEffects() { //clean side effects
        authHandler.numberFailedLogins = 0
        authHandler.totalRequests = 0
        jsonObjectGetHandler.requestsTotal = 0
        reset()
        handlers.forEach(Consumer { handler: HttpHandlers.HttpHandlerBase ->
            handler.requests_total = 0
        })
        handlers = ArrayList()
        server.stop(0)
        TimeStorage.instance.reset()
        AssertionStorage.instance.reset()
        Thread.sleep(200)
    }

    companion object {
        private val log = LogManager.getLogger(
            RequestHandlingFramework::class.java
        )
    }
}