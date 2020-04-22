package de.hpi.tdgt

import com.fasterxml.jackson.databind.ObjectMapper
import de.hpi.tdgt.controllers.UploadController
import de.hpi.tdgt.deserialisation.Deserializer.deserialize
import de.hpi.tdgt.requesthandling.RestClient
import de.hpi.tdgt.test.story.UserStory
import kotlinx.coroutines.runBlocking
import org.apache.commons.io.IOUtils
import org.apache.logging.log4j.LogManager
import java.io.FileInputStream
import java.io.IOException
import java.io.StringWriter
import java.net.URL
import java.util.concurrent.ExecutionException
import kotlin.collections.HashMap

object CLI {
    const val USERNAME = "superuser"
    const val PASSWORD = "somepw"
    private val log = LogManager.getLogger(CLI::class.java)

    @JvmStatic
    fun restTest() {
        runBlocking {
            val rc = RestClient()
            val params = HashMap<String, String>()
            params["username"] = USERNAME
            params["password"] = PASSWORD
            log.info("--- Testing user creation and update ---")
            var result = rc.postBodyToEndpoint(
                UserStory(),
                "Endpoint test", 0,
                URL("http://users/users/new"),
                emptyArray(),
                HashMap(),
                ObjectMapper().writeValueAsString(params)
            )
            log.info("Create user: " + result.toString() + " and code: " + result!!.returnCode + " in: " + result.durationMillis() + " ms.")
            result = rc.getFromEndpointWithAuth(
                UserStory(),
                "Endpoint test", 0,
                URL("http://users/users/all"),
                emptyArray(),
                HashMap(),
                HashMap(),
                USERNAME,
                PASSWORD
            )
            log.info("Get all users: " + result.toString() + " and code: " + result!!.returnCode + " in: " + result.durationMillis() + " ms.")
            result = rc.putFormToEndpointWithAuth(
                UserStory(),
                "Endpoint test", 0,
                URL("http://users/users/update"),
                emptyArray(),
                params,
                HashMap(),
                USERNAME,
                PASSWORD
            )
            log.info("Update user: " + result.toString() + " and code: " + result!!.returnCode + " in: " + result.durationMillis() + " ms.")
            log.info("--- Testing post creation ---")
            params.clear()
            params["title"] = "A very good post"
            params["text"] = "because it is rather short."
            result = rc.postFormToEndpointWithAuth(
                UserStory(),
                "Endpoint test", 0,
                URL("http://posts/posts/new"),
                emptyArray(),
                params,
                HashMap(),
                USERNAME,
                PASSWORD
            )
            log.info("Create post: " + result.toString() + " and code: " + result!!.returnCode + " in: " + result.durationMillis() + " ms.")
            result = rc.getFromEndpointWithAuth(
                UserStory(),
                "Endpoint test", 0,
                URL("http://posts/posts/all"),
                emptyArray(),
                HashMap(),
                HashMap(),
                USERNAME,
                PASSWORD
            )
            log.info("Get all posts: " + result.toString() + " and code: " + result!!.returnCode + " in: " + result.durationMillis() + " ms.")
            log.info("--- Testing search ---")
            params.clear()
            params["key"] = "short"
            result = rc.getFromEndpointWithAuth(
                UserStory(),
                "Endpoint test", 0,
                URL("http://search/posts/search"),
                emptyArray(),
                params,
                HashMap(),
                USERNAME,
                PASSWORD
            )
            log.info("Search: " + result.toString() + " and code: " + result!!.returnCode + " in: " + result.durationMillis() + " ms.")
            log.info("--- Deleting user ---")
            result = rc.deleteFromEndpointWithAuth(
                UserStory(),
                "Endpoint test", 0,
                URL("http://users/users/delete"),
                emptyArray(),
                HashMap(),
                HashMap(),
                USERNAME,
                PASSWORD
            )
            log.info("Delete user: " + result.toString() + " and code: " + result!!.returnCode + " in: " + result.durationMillis() + " ms.")
        }
    }

    @JvmStatic
    public fun loadTest(from: String) {
        try {
            val writer = StringWriter()
            IOUtils.copy(FileInputStream(from), writer)
            val json = writer.toString()
            val deserializedTest = deserialize(json)
            log.info("Successfully deserialized input json including " + deserializedTest.getStories().size + " stories.")
            log.info("Running test...")
            runBlocking {
                //in case warmup is added
                val controller = UploadController()
                controller.uploadTestConfig(json, System.currentTimeMillis())
                controller.currentThread?.join()
            }
        } catch (e: IOException) {
            log.error(e)
        } catch (e: ExecutionException) {
            log.error(e)
        }
    }
}