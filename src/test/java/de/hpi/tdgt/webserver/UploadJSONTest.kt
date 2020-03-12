package de.hpi.tdgt.webserver

import de.hpi.tdgt.RequestHandlingFramework
import de.hpi.tdgt.Utils
import de.hpi.tdgt.WebApplication
import org.apache.logging.log4j.LogManager
import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.RequestEntity
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.io.IOException
import java.net.URL

@ExtendWith(SpringExtension::class)
@SpringBootTest(classes = [WebApplication::class], webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UploadJSONTest : RequestHandlingFramework() {
    @LocalServerPort
    private val port = 0
    private lateinit var exampleStory: String
    @BeforeEach
    @Throws(IOException::class)
    fun prepare() {
        exampleStory = Utils().requestExampleJSON
    }

    @Autowired
    private val restTemplate: TestRestTemplate? = null

    @Test
    @Throws(Exception::class)
    fun runsUserStoryAgainstTestServerReturns200() {
        val requestEntity =
            RequestEntity.post(URL("http://localhost:" + port + "/upload/" + System.currentTimeMillis()).toURI())
                .contentType(MediaType.APPLICATION_JSON).body(exampleStory)
        val response =
            restTemplate!!.exchange(requestEntity, String::class.java)
        MatcherAssert.assertThat(
            response.statusCode,
            Matchers.equalTo(HttpStatus.OK)
        )
    }

    @Test
    @Throws(Exception::class)
    fun runsUserStoryAgainstTestServerRunsActualTest() {
        val requestEntity =
            RequestEntity.post(URL("http://localhost:" + port + "/upload/" + System.currentTimeMillis()).toURI())
                .contentType(MediaType.APPLICATION_JSON).body(exampleStory)
        restTemplate!!.exchange(requestEntity, String::class.java)
        //requests to this handler are sent
        MatcherAssert.assertThat(authHandler.numberFailedLogins, Matchers.greaterThan(0))
    }

    @Test
    @Throws(Exception::class)
    fun runsUserStoryAgainstTestServerRunsActualTestAlsoInCliMode() {
        val args = arrayOf(
            "cli",
            "load",
            "./src/test/resources/de/hpi/tdgt/RequestExample.json",
            "./src/test/resources/de/hpi/tdgt",
            "./src/test/resources/de/hpi/tdgt"
        )
        WebApplication.main(args)
        //requests to this handler are sent
        MatcherAssert.assertThat(authHandler.numberFailedLogins, Matchers.greaterThan(0))
    }

    @Test
    @Throws(Exception::class)
    fun runsUserStoryAgainstTestServerReturns415OnWrongContentType() {
        val requestEntity =
            RequestEntity.post(URL("http://localhost:" + port + "/upload/" + System.currentTimeMillis()).toURI())
                .contentType(MediaType.APPLICATION_PDF).body(exampleStory)
        val response =
            restTemplate!!.exchange(requestEntity, String::class.java)
        MatcherAssert.assertThat(
            response.statusCode,
            Matchers.equalTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
        )
    }

    @Test
    @Throws(Exception::class)
    fun runsUserStoryAgainstTestServerReturns400OnNotJSON() {
        val requestEntity =
            RequestEntity.post(URL("http://localhost:" + port + "/upload/" + System.currentTimeMillis()).toURI())
                .contentType(MediaType.APPLICATION_JSON).body("{")
        val response =
            restTemplate!!.exchange(requestEntity, String::class.java)
        MatcherAssert.assertThat(
            response.statusCode,
            Matchers.equalTo(HttpStatus.BAD_REQUEST)
        )
    }

    @Test
    @Throws(Exception::class)
    fun runsUserStoryAgainstTestServerReturns400OnNoContent() {
        val requestEntity =
            RequestEntity.post(URL("http://localhost:" + port + "/upload/" + System.currentTimeMillis()).toURI())
                .contentType(MediaType.APPLICATION_JSON).body("")
        val response =
            restTemplate!!.exchange(requestEntity, String::class.java)
        MatcherAssert.assertThat(
            response.statusCode,
            Matchers.equalTo(HttpStatus.BAD_REQUEST)
        )
    }

    companion object {
        private val log = LogManager.getLogger(
            UploadJSONTest::class.java
        )
    }
}