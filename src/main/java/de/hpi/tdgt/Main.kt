package de.hpi.tdgt

import com.fasterxml.jackson.databind.ObjectMapper
import de.hpi.tdgt.controllers.UploadController
import de.hpi.tdgt.deserialisation.Deserializer.deserialize
import de.hpi.tdgt.requesthandling.RestClient
import de.hpi.tdgt.test.story.atom.Data_Generation
import org.apache.commons.io.IOUtils
import org.apache.logging.log4j.LogManager
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.StringWriter
import java.net.URISyntaxException
import java.net.URL
import java.util.*
import java.util.concurrent.ExecutionException

object Main {
    const val USERNAME = "superuser"
    const val PASSWORD = "somepw"
    private val log = LogManager.getLogger(Main::class.java)
    @JvmStatic
    @Throws(IOException::class, InterruptedException::class)
    fun main(args: Array<String>) {
        if (args.size == 1) {
            try {
                log.error("Usage: java -jar " + File(Main::class.java.protectionDomain.codeSource.location.toURI()).name + " cli load <Path to request JSON> <Path to generated Data>")
                log.error("or: java -jar " + File(Main::class.java.protectionDomain.codeSource.location.toURI()).name + " cli testRest")
                System.exit(1)
            } catch (e: URISyntaxException) {
                log.error(e)
            }
        }
        if (args[1] == "load") {
            try {
                val writer = StringWriter()
                IOUtils.copy(FileInputStream(args[2]), writer)
                val json = writer.toString()
                val deserializedTest = deserialize(json)
                log.info("Successfully deserialized input json including " + deserializedTest.getStories().size + " stories.")
                log.info("Running test...")
                Data_Generation.outputDirectory = args[3]
                UploadController.PDGF_DIR = args[3]
                UploadController.JAVA_7_DIR = args[4]
                //in case warmup is added
                UploadController().uploadTestConfig(json, System.currentTimeMillis())
            } catch (e: IOException) {
                log.error(e)
            } catch (e: ExecutionException) {
                log.error(e)
            }
        } else {
            val rc = RestClient()
            val params = HashMap<String, String>()
            params["username"] = USERNAME
            params["password"] = PASSWORD
            log.info("--- Testing user creation and update ---")
            var result = rc.postBodyToEndpoint(
                "REST Test",
                0,
                URL("http://users/users/new"),
                ObjectMapper().writeValueAsString(params)
            )
            log.info("Create user: " + result.toString() + " and code: " + result!!.returnCode + " in: " + result.durationMillis() + " ms.")
            result = rc.getFromEndpointWithAuth(
                "REST Test",
                0,
                URL("http://users/users/all"),
                HashMap(),
                USERNAME,
                PASSWORD
            )
            log.info("Get all users: " + result.toString() + " and code: " + result!!.returnCode + " in: " + result.durationMillis() + " ms.")
            result = rc.putFormToEndpointWithAuth(
                "REST Test",
                0,
                URL("http://users/users/update"),
                params,
                USERNAME,
                PASSWORD
            )
            log.info("Update user: " + result.toString() + " and code: " + result!!.returnCode + " in: " + result.durationMillis() + " ms.")
            log.info("--- Testing post creation ---")
            params.clear()
            params["title"] = "A very good post"
            params["text"] = "because it is rather short."
            result = rc.postFormToEndpointWithAuth(
                "REST Test",
                0,
                URL("http://posts/posts/new"),
                params,
                USERNAME,
                PASSWORD
            )
            log.info("Create post: " + result.toString() + " and code: " + result!!.returnCode + " in: " + result.durationMillis() + " ms.")
            result = rc.getFromEndpointWithAuth(
                "REST Test",
                0,
                URL("http://posts/posts/all"),
                HashMap(),
                USERNAME,
                PASSWORD
            )
            log.info("Get all posts: " + result.toString() + " and code: " + result!!.returnCode + " in: " + result.durationMillis() + " ms.")
            log.info("--- Testing search ---")
            params.clear()
            params["key"] = "short"
            result = rc.getFromEndpointWithAuth(
                "REST Test",
                0,
                URL("http://search/posts/search"),
                params,
                USERNAME,
                PASSWORD
            )
            log.info("Search: " + result.toString() + " and code: " + result!!.returnCode + " in: " + result.durationMillis() + " ms.")
            log.info("--- Deleting user ---")
            result = rc.deleteFromEndpointWithAuth(
                "REST Test",
                0,
                URL("http://users/users/delete"),
                HashMap(),
                USERNAME,
                PASSWORD
            )
            log.info("Delete user: " + result.toString() + " and code: " + result!!.returnCode + " in: " + result.durationMillis() + " ms.")
        }
    }
}