package de.hpi.tdgt.webserver

import com.ginsberg.junit.exit.ExpectSystemExitWithStatus
import de.hpi.tdgt.RequestHandlingFramework
import de.hpi.tdgt.Utils
import de.hpi.tdgt.WebApplication
import de.hpi.tdgt.controllers.UploadController
import de.hpi.tdgt.util.PropertiesReader
import org.apache.logging.log4j.LogManager
import org.eclipse.paho.client.mqttv3.IMqttMessageListener
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import org.hamcrest.MatcherAssert
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers
import org.hamcrest.Matchers.equalTo
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
import java.lang.Thread.sleep
import java.net.URL
import java.util.*

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

    private fun waitForTestEnd(){
        var finished = false;
        val publisherId = UUID.randomUUID().toString()
        val publisher = MqttClient(PropertiesReader.getMqttHost(), publisherId, MemoryPersistence())
        val options = MqttConnectOptions()
        options.isAutomaticReconnect = true
        options.isCleanSession = true
        options.connectionTimeout = 10
        (publisher as MqttClient).connect(options)
        publisher.subscribe(de.hpi.tdgt.test.Test.MQTT_TOPIC, IMqttMessageListener { s: String, mqttMessage: MqttMessage ->
            //hamcrest can't handle empty sets in the list for contains, so filter them out
            if(String(mqttMessage.payload).startsWith("testEnd")){
                finished = true
            }
        })

        while(!finished){
            sleep(1000)
        }
    }

    @Test
    @Throws(Exception::class)
    fun runsUserStoryAgainstTestServerRunsActualTest() {
        val requestEntity =
            RequestEntity.post(URL("http://localhost:" + port + "/upload/" + System.currentTimeMillis()).toURI())
                .contentType(MediaType.APPLICATION_JSON).body(exampleStory)
        restTemplate!!.exchange(requestEntity, String::class.java)
        //test is run async
        waitForTestEnd()
        //requests to this handler are sent
        MatcherAssert.assertThat(authHandler.numberFailedLogins, Matchers.greaterThan(0))
    }
    @Test
    @Throws(Exception::class)
    @ExpectSystemExitWithStatus(0)
    fun runsUserStoryAgainstTestServerRunsActualTestAlsoInCliMode() {
        //Since everything is fine, the application should exit with 0
        val args = arrayOf(
            "--load",
            "./src/test/resources/de/hpi/tdgt/RequestExample.json",
            "./src/test/resources/de/hpi/tdgt",
            "./src/test/resources/de/hpi/tdgt"
        )
        WebApplication.main(args)
        //test is run async
        waitForTestEnd()
        //requests to this handler are sent
        MatcherAssert.assertThat(authHandler.numberFailedLogins, Matchers.greaterThan(0))
    }

    @Test
    @Throws(Exception::class)
    @ExpectSystemExitWithStatus(1)
    fun doesNotStartIfNoValidURL() {
        //Since everything is fine, the application should exit with 0
        val args = arrayOf(
            "--location",
            "http://",
            "./src/test/resources/de/hpi/tdgt",
            "./src/test/resources/de/hpi/tdgt"
        )
        WebApplication.main(args)
    }

    @Test
    @Throws(Exception::class)
    @ExpectSystemExitWithStatus(0)
    fun setsValidURL() {
        //Since everything is fine, the application should exit with 0
        val args = arrayOf(
            "--location",
            "http://localhost:8080",
            "--load",
            "./src/test/resources/de/hpi/tdgt/RequestExample.json",
            "./src/test/resources/de/hpi/tdgt",
            "./src/test/resources/de/hpi/tdgt"
        )
        WebApplication.main(args)
        assertThat(UploadController.LOCATION,equalTo("http://localhost:8080"))
    }

    @Test
    @Throws(Exception::class)
    @ExpectSystemExitWithStatus(1)
    fun ExitsWith1IfUnallowedParametersInCliMode() {
        val args = arrayOf(
            "cli"
        )
        WebApplication.main(args)
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