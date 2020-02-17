package de.hpi.tdgt

import com.sun.net.httpserver.HttpServer
import de.hpi.tdgt.HttpHandlers.*
import de.hpi.tdgt.test.Test
import de.hpi.tdgt.test.story.atom.Data_Generation
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
    protected val getHandler = GetHandler()
    protected val getWithBodyHandler = GetWithBodyHandler()
    protected val jsonObjectGetHandler = JSONObjectGetHandler()
    protected val jsonArrayGetHandler = JSONArrayGetHandler()
    protected val postHandler = PostHandler()
    @JvmField
    protected val postBodyHandler = PostBodyHandler()
    protected val putBodyHandler = PostBodyHandler()
    @JvmField
    protected val authHandler = AuthHandler()
    protected val emptyResponseHandler = EmptyResponseHandler()
    protected var server: HttpServer? = null
    @JvmField
    protected var handlers: MutableList<HttpHandlerBase> = ArrayList()
    //Based on https://www.codeproject.com/tips/1040097/create-a-simple-web-server-in-java-http-server
    @BeforeEach
    @Throws(IOException::class)
    fun launchTestServer() {
        val port = 9000
        server = HttpServer.create(InetSocketAddress(port), 0)
        log.info("server started at $port")
        server!!.createContext("/", getHandler)
        server!!.createContext("/getWithBody", getWithBodyHandler)
        server!!.createContext("/jsonObject", jsonObjectGetHandler)
        server!!.createContext("/jsonArray", jsonArrayGetHandler)
        server!!.createContext("/echoPost", postHandler)
        server!!.createContext("/postWithBody", postBodyHandler)
        server!!.createContext("/putWithBody", putBodyHandler)
        server!!.createContext("/auth", authHandler)
        server!!.createContext("/empty", emptyResponseHandler)
        server!!.setExecutor(null)
        server!!.start()
        handlers.add(getHandler)
        handlers.add(getWithBodyHandler)
        handlers.add(jsonObjectGetHandler)
        handlers.add(jsonArrayGetHandler)
        handlers.add(postHandler)
        handlers.add(postBodyHandler)
        handlers.add(putBodyHandler)
        handlers.add(authHandler)
        handlers.add(emptyResponseHandler)
        val values = File("values.csv")
        values.deleteOnExit()
        val os = FileOutputStream(values)
        IOUtils.copy(Utils().valuesCSV, os)
        os.close()
        //tests want predictable behaviour in regards to when an entry is stored
        AssertionStorage.getInstance().isStoreEntriesAsynch = false
    }

    @AfterEach
    fun removeSideEffects() { //clean side effects
        authHandler.numberFailedLogins = 0
        authHandler.totalRequests = 0
        jsonObjectGetHandler.requestsTotal = 0
        Data_Generation.reset()
        handlers.forEach(Consumer { handler: HttpHandlerBase ->
            handler.setRequests_total(
                0
            )
        })
        handlers = ArrayList()
        server!!.stop(0)
        TimeStorage.getInstance().reset()
        AssertionStorage.getInstance().reset()
        Test.ConcurrentRequestsThrottler.instance.reset()
    }

    companion object {
        private val log = LogManager.getLogger(
            RequestHandlingFramework::class.java
        )
    }
}