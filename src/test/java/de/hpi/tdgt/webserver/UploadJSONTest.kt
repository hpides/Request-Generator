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

package de.hpi.tdgt.webserver

import com.ginsberg.junit.exit.ExpectSystemExitWithStatus
import de.hpi.tdgt.RequestHandlingFramework
import de.hpi.tdgt.Utils
import de.hpi.tdgt.WebApplication
import de.hpi.tdgt.controllers.UploadController
import de.hpi.tdgt.test.story.atom.DataGeneration
import de.hpi.tdgt.util.PropertiesReader
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.apache.logging.log4j.LogManager
import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.AfterEach
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
import java.util.concurrent.ExecutionException
import kotlin.collections.HashSet

@ExtendWith(SpringExtension::class)
@SpringBootTest(classes = [WebApplication::class], webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UploadJSONTest : RequestHandlingFramework() {
    @LocalServerPort
    private val port = 0
    private lateinit var exampleStory: String

    private var dataGenerationDir:String = ""

    @BeforeEach
    @Throws(IOException::class)
    fun prepare() {
        exampleStory = Utils().requestExampleJSON
        val options = MqttConnectOptions()
        options.isAutomaticReconnect = true
        options.isCleanSession = true
        options.connectionTimeout = 10
        publisher.connect(options)
        dataGenerationDir = DataGeneration.outputDirectory
    }
    @AfterEach
    fun closeDown(){
        publisher.disconnect()
        DataGeneration.outputDirectory = dataGenerationDir
    }

    @Autowired
    private val restTemplate: TestRestTemplate? = null

    @Autowired
    private lateinit var uploadController:UploadController;

    @Test
    @Throws(Exception::class)
    fun runsUserStoryAgainstTestServerReturns200() {
        val requestEntity =
            RequestEntity.post(URL("http://localhost:" + port + "/upload/" + System.currentTimeMillis()).toURI())
                .contentType(MediaType.APPLICATION_JSON).body(exampleStory)
        val response =
            restTemplate!!.exchange(requestEntity, String::class.java)
        // else there will be side effects on other tests
        waitForTestEnd()
        assertThat(
            response.statusCode,
            equalTo(HttpStatus.OK)
        )
    }
    val publisherId = UUID.randomUUID().toString()
    val publisher = MqttClient(PropertiesReader.getMqttHost(), publisherId, MemoryPersistence())
    private fun waitForTestState(state:String){
        var finished = false;
        publisher.subscribe(de.hpi.tdgt.test.Test.MQTT_TOPIC, IMqttMessageListener { s: String, mqttMessage: MqttMessage ->
            //hamcrest can't handle empty sets in the list for contains, so filter them out
            if(String(mqttMessage.payload).startsWith(state)){
                finished = true
            }
        })

        while(!finished){
            sleep(1000)
        }
    }

    private fun waitForTestEnd(){
        waitForTestState("testEnd")
    }
    private fun waitForTestStart(){
        waitForTestState("testStart")
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
        assertThat(authHandler.numberFailedLogins, Matchers.greaterThan(0))
    }

    @Test
    @Throws(Exception::class)
    fun runningTestCanBeAborted() {
        sleep(5000)
        val id = System.currentTimeMillis()
        val requestEntity =
            RequestEntity.post(URL("http://localhost:$port/upload/$id").toURI())
                .contentType(MediaType.APPLICATION_JSON).body(Utils().requestExampleJSONWithDelay)
        restTemplate!!.exchange(requestEntity, String::class.java)
        //test is run async, if aborted should finish immediately
        publisher.publish(de.hpi.tdgt.test.Test.MQTT_TOPIC, ("abort $id").toByteArray(), 2, false)

        waitForTestEnd()

        //not all requests to this handler can be sent by now
        assertThat(authHandler.totalRequests, Matchers.lessThan(37))
    }

    @Test
    @Throws(Exception::class)
    fun runsUserStoryAgainstTestServerRunsActualTestWhenDistributed() {
        UploadController.LOCATION = "http://localhost:$port"
        val requestEntity =
            RequestEntity.post(URL("http://localhost:" + port + "/upload/" + System.currentTimeMillis()+"/distributed").toURI())
                .contentType(MediaType.APPLICATION_JSON).body(exampleStory)
        val response = restTemplate!!.exchange(requestEntity, String::class.java)
        assertThat(response.statusCode, equalTo(HttpStatus.OK))
        //test is run async
        waitForTestEnd()
        //requests to this handler are sent
        assertThat(authHandler.numberFailedLogins, Matchers.greaterThan(0))
    }

    @Test
    @Throws(Exception::class)
    fun canDetectOwnLocation() {
        assertThat("UploadController should have been initialised by test framework", ::uploadController.isInitialized)
        assertThat(uploadController.defaultLocation(), Matchers.equalTo("http://localhost:$port"));
    }

    @Test
    @Throws(Exception::class)
    fun throwsErrorIfNoNodesAvailable() {
        //assuming there are no active nodes in the network, this leads to UploadController finding no nodes
        UploadController.LOCATION = null
        val requestEntity =
            RequestEntity.post(URL("http://localhost:" + port + "/upload/" + System.currentTimeMillis()+"/distributed").toURI())
                .contentType(MediaType.APPLICATION_JSON).body(exampleStory)
        val response = restTemplate!!.exchange(requestEntity, String::class.java)
        assertThat(response.statusCode, equalTo(HttpStatus.INTERNAL_SERVER_ERROR))
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
        assertThat(authHandler.numberFailedLogins, Matchers.greaterThan(0))
    }

    @Test
    @Throws(Exception::class)
    @ExpectSystemExitWithStatus(3)
    fun exitsWithThreeFromCLIModeIfAssertionViolated() {
        //the test contains a request to a non-existant host, so an implicit assertion should fail
        val args = arrayOf(
            "--load",
            "./src/test/resources/de/hpi/tdgt/RequestFailureExample.json",
            "./src/test/resources/de/hpi/tdgt",
            "./src/test/resources/de/hpi/tdgt"
        )
        WebApplication.main(args)
        //test is run async
        waitForTestEnd()
    }

    @Test
    @Throws(Exception::class)
    @ExpectSystemExitWithStatus(2)
    fun exitsWithTwoFromCLIModeIfRequestTakesTooLong() {
        //the test contains a request to a non-existant host, so an implicit assertion should fail
        val args = arrayOf(
            "--load",
            "./src/test/resources/de/hpi/tdgt/RequestTooLongExample.json",
            "./src/test/resources/de/hpi/tdgt",
            "./src/test/resources/de/hpi/tdgt"
        )
        WebApplication.main(args)
        //test is run async
        waitForTestEnd()
    }

    @Test
    @Throws(Exception::class)
    @ExpectSystemExitWithStatus(5)
    fun exitsWithFiveFromCLIModeIfRequestTakesTooLongAndAssertionsAreViolated() {
        //the test contains a request to a non-existant host, so an implicit assertion should fail
        val args = arrayOf(
            "--load",
            "./src/test/resources/de/hpi/tdgt/RequestTooLongFailureExample.json",
            "./src/test/resources/de/hpi/tdgt",
            "./src/test/resources/de/hpi/tdgt"
        )
        WebApplication.main(args)
        //test is run async
        waitForTestEnd()
    }

    @Test
    @Throws(Exception::class)
    @ExpectSystemExitWithStatus(0)
    fun passesUnknownParamsToSpringBoot() {
        //Since everything is fine, the application should exit with 0
        val args = arrayOf(
            "-Dserver.port=8090",
            "--load",
            "./src/test/resources/de/hpi/tdgt/RequestExample.json",
            "./src/test/resources/de/hpi/tdgt",
            "./src/test/resources/de/hpi/tdgt"
        )
        WebApplication.main(args)
        //test is run async
        waitForTestEnd()
        //requests to this handler are sent
        assertThat(authHandler.numberFailedLogins, Matchers.greaterThan(0))
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
    fun canChangeDefaultPDGFOutputDirDuringRunTime() {
        //Since everything is fine, the application should exit with 0
        val args = arrayOf(
                "--alternative-output-dir",
                "/output",
            "--noop",
            "./src/test/resources/de/hpi/tdgt",
            "./src/test/resources/de/hpi/tdgt"
        )
        WebApplication.main(args)
        assertThat(DataGeneration.outputDirectory, equalTo("/output"))
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
    fun setsBrokerURL() {
        //Since everything is fine, the application should exit with 0
        val args = arrayOf(
            "--broker-url",
            "mqtt://mosquitto:1883",
            "--noop",
            "./src/test/resources/de/hpi/tdgt",
            "./src/test/resources/de/hpi/tdgt"
        )
        WebApplication.main(args)
        assertThat(PropertiesReader.BROKER_URL,equalTo("mqtt://mosquitto:1883"))
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
        assertThat(
            response.statusCode,
            equalTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
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
        assertThat(
            response.statusCode,
            equalTo(HttpStatus.BAD_REQUEST)
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
        assertThat(
            response.statusCode,
            equalTo(HttpStatus.BAD_REQUEST)
        )
    }
    @Autowired
    private val controller:UploadController? = null;

    @org.junit.jupiter.api.Test
    @Throws(
        MqttException::class,
        InterruptedException::class,
        ExecutionException::class,
        IOException::class
    )
    fun masterNodeCollectsWorkerNodes() {
        runBlocking {
            UploadController.LOCATION = null
            val job = GlobalScope.launch { controller!!.uploadTestConfigForDistributed("{}",0) }
            val urls = HashSet<String>()
            for(i in 1..20){
                publisher.publish(de.hpi.tdgt.test.Test.MQTT_TOPIC, MqttMessage((UploadController.IDENTIFICATION_RESPONSE_MESSAGE + " " +i).toByteArray()))
                urls.add(i.toString())
            }
            job.join()
            assertThat(controller!!.knownOtherInstances, equalTo(urls))
        }
    }

    companion object {
        private val log = LogManager.getLogger(
            UploadJSONTest::class.java
        )
    }
}