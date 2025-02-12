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

package de.hpi.tdgt.controllers

import com.fasterxml.jackson.databind.ObjectMapper
import de.hpi.tdgt.concurrency.Event
import de.hpi.tdgt.deserialisation.Deserializer
import de.hpi.tdgt.requesthandling.RestClient
import de.hpi.tdgt.test.Test
import de.hpi.tdgt.test.story.atom.Request
import de.hpi.tdgt.test.story.atom.assertion.Assertion
import de.hpi.tdgt.test.story.atom.assertion.AssertionStorage
import de.hpi.tdgt.test.time_measurement.TimeStorage
import de.hpi.tdgt.util.PropertiesReader
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.apache.logging.log4j.LogManager
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.web.context.WebServerInitializedEvent
import org.springframework.context.ApplicationListener
import org.springframework.http.*
import org.springframework.web.bind.annotation.*
import org.springframework.web.client.RestTemplate
import java.io.*
import java.lang.Thread.sleep
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.concurrent.ExecutionException
import java.util.concurrent.Future
import kotlin.collections.HashSet
import kotlin.system.exitProcess


@RestController
class UploadController : ApplicationListener<WebServerInitializedEvent> {
    val client:MqttClient
    val knownOtherInstances = HashSet<String>()
    init {
        val publisherId = UUID.randomUUID().toString()
        //use memory persistence because it is not important that all packets are transferred and we do not want to spam the file system
        this.client = MqttClient(PropertiesReader.getMqttHost(), publisherId, MemoryPersistence())
        val options = MqttConnectOptions()
        options.isAutomaticReconnect = true
        options.isCleanSession = true
        options.connectionTimeout = 10
        client.connect(options)
        client.subscribe(Test.MQTT_TOPIC) { topic: String?, message: MqttMessage? ->
            run { if (topic == Test.MQTT_TOPIC && message != null) {
                    val request = String(message.payload)
                    if (request.startsWith(IDENTIFICATION_REQUEST_MESSAGE)) {
                        if (LOCATION == null) {
                            log.warn("Was requested to join in a test run but I do not know my location!")
                        } else {
                            client.publish(
                                Test.MQTT_TOPIC,
                                ("$IDENTIFICATION_RESPONSE_MESSAGE $LOCATION").toByteArray(),
                                1,
                                false
                            )
                        }
                    }
                    else if(request.startsWith(IDENTIFICATION_RESPONSE_MESSAGE)){
                        //format is header space host
                        val parts = request.split(" ")
                        if(parts.size > 1) {
                            val host = parts[1]
                            knownOtherInstances.add(host)
                        }else{
                            log.error("Discovery Protocol violation: identify messages must be the word identify followed by a space and a URL")
                        } }
                    else if(request.startsWith(ABORT_MESSAGE)){
                        //format is abort testid
                        val parts = request.split(" ")
                        if(parts.size > 1) {
                            val testID = parts[1]
                            try {
                                val id = testID.toLong()
                                //will terminate test once one is started and ID is correct
                                GlobalScope.launch {
                                    Test.abortTest(id)
                                }
                            } catch (e: NumberFormatException){
                                log.error("Abort protocol violation: test id $testID is no valid ID since no number!")
                            }
                        }else{
                            log.error("Abort Protocol violation: abort messages must be the word abort followed by a space and an ID")
                        }
                    }
                }
            }
        }
    }
    //will return 500 if exception during test occurs
    @PostMapping(path = ["/upload/{id}"], consumes = [MediaType.APPLICATION_JSON_VALUE])
    @Throws(
        InterruptedException::class, ExecutionException::class
    )
    fun uploadTestConfig(@RequestBody testToRunAsJSON: String?, @PathVariable(required = false) id: Long): ResponseEntity<String> {

        val testToRun: Test
        //Jackson might throw different kinds of exceptions, depending on the error
        testToRun = try {
            Deserializer.deserialize(testToRunAsJSON)
        } catch (e: IllegalArgumentException) {
            log.error(e)
            return ResponseEntity(HttpStatus.BAD_REQUEST)
        } catch (e: IOException) {
            log.error(e)
            return ResponseEntity(HttpStatus.BAD_REQUEST)
        }
        val ret = ResponseEntity<String>(HttpStatus.OK)
        currentThread = Thread {runTest(testToRun, id)}
        currentThread!!.start()
        return ret
    }
    val template = RestTemplate()
    val mapper = ObjectMapper()
    // reports the number of nodes it could aquire to run this test
    @PostMapping(path = ["/upload/{id}/distributed"], consumes = [MediaType.APPLICATION_JSON_VALUE])
    @Throws(
        InterruptedException::class, ExecutionException::class
    )
    fun uploadTestConfigForDistributed(@RequestBody testToRunAsJSON: String?, @PathVariable(required = false) id: Long): ResponseEntity<Int> {
        val test = mapper.readValue(testToRunAsJSON, Object::class.java)
        var allNodesAccepted = true
        //data might be outdated by now
        knownOtherInstances.clear()
        //clients can receive request multiple times; retain this message so that nodes that are "late to the party" can still reply
        client.publish(Test.MQTT_TOPIC, IDENTIFICATION_REQUEST_MESSAGE.toByteArray(),1,true)
        sleep(DISCOVERY_TIMEOUT_MS)
        val allNodes = knownOtherInstances.toTypedArray()
        //make sure that we have same order later when Test is run
        allNodes.sort()
        if(allNodes.isEmpty()){
            log.error("Did not find a single node including myself --> abort!!")
            return ResponseEntity(0,HttpStatus.INTERNAL_SERVER_ERROR)
        }
        for(i in allNodes.indices){
            var addr = allNodes[i]
            addr = "$addr/upload/${id}/distributed/${i}/of/${allNodes.size}"
            try {
                val response = template.postForEntity(URL(addr).toURI(), test, String::class.java)
                if (response.statusCode != HttpStatus.OK) {
                    log.error("Node $addr responded with code ${response.statusCode}")
                } else {
                    continue
                }
            } catch (e:Exception){
                log.error("Could not connect to node $addr ",e)
            }
            allNodesAccepted = false
        }
        return  if(allNodesAccepted){ResponseEntity(knownOtherInstances.size,HttpStatus.OK)} else{ResponseEntity(0,HttpStatus.INTERNAL_SERVER_ERROR)}
    }

    //runs a test as a single node
    @PostMapping(path = ["/upload/{id}/distributed/{nodenumber}/of/{nodes}"], consumes = [MediaType.APPLICATION_JSON_VALUE])
    @Throws(
        InterruptedException::class, ExecutionException::class
    )
    fun uploadTestConfigForOneDistributedNode(@RequestBody testToRunAsJSON: String?, @PathVariable(required = true) id: Long, @PathVariable(required = true) nodenumber: Long, @PathVariable(required = true) nodes: Long): ResponseEntity<String> {
        val testToRun: Test
        //Jackson might throw different kinds of exceptions, depending on the error
        testToRun = try {
            Deserializer.deserialize(testToRunAsJSON)
        } catch (e: IllegalArgumentException) {
            log.error(e)
            return ResponseEntity(HttpStatus.BAD_REQUEST)
        } catch (e: IOException) {
            log.error(e)
            return ResponseEntity(HttpStatus.BAD_REQUEST)
        }
        val ret = ResponseEntity<String>(HttpStatus.OK)
        //e.g. to adapt the scale factor
        testToRun.nodes = nodes
        testToRun.nodeNumber = nodenumber
        currentThread = Thread {runTest(testToRun, id)}
        currentThread!!.start()
        return ret
    }

    /**
     * In some scenarios, e.g. CLI mode, we need to wait for the thread with the test to terminate.
     */
    var currentThread: Thread? = null
    private fun runTest(testToRun: Test, id: Long) {
        testToRun.testId = id
        runBlocking {
            Event.reset()
            //no assertions run yet
            Assertion.oneHasFailed = false
            Request.oneExceededThreshold = false
            //could not have happened yet
            val threads: MutableCollection<Future<*>> = testToRun.warmup()
            testToRun.start(threads)
        }
        val endtime = System.currentTimeMillis()
        log.error("---Test finished in " + (endtime - testToRun.testStart) + " ms.---")
        log.error("---Times---")
        TimeStorage.instance.printSummary()
        log.error("---Assertions---")
        AssertionStorage.instance.printSummary()
        TimeStorage.instance.reset()
        AssertionStorage.instance.reset()
        log.error(RestClient.requestsSent.get().toString() + " requests sent.")
        RestClient.requestsSent.set(0)
        currentThread = null
    }
    //will return 500 if exception during test occurs
    @PostMapping(
        path = ["/uploadPDGF"],
        consumes = [MediaType.APPLICATION_XML_VALUE],
        produces = [MediaType.TEXT_PLAIN_VALUE]
    )
    @Throws(
        InterruptedException::class, ExecutionException::class
    )
    fun uploadDataGenConfig(@RequestBody pdgfConfig: String?): ResponseEntity<String> {
        val starttime = System.currentTimeMillis()
        //store uploaded config in temporary file, so multiple instances could run concurrently
        val tempFile: File
        try {
            tempFile = File.createTempFile("pdgf", ".xml")
            tempFile.deleteOnExit()
            val writer = FileWriter(tempFile)
            writer.write(pdgfConfig)
            writer.close()
        } catch (e: IOException) {
            log.error(e)
            return ResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR)
        }
        val output = StringBuilder()
        try {
            val pdgfProcess = ProcessBuilder(
                JAVA_7_DIR,
                "-jar",
                PDGF_DIR + File.separator + "pdgf.jar",
                "-l",
                tempFile.absolutePath,
                "-l",
                PDGF_DIR + File.separator + "config" + File.separator + "customer-output.xml",
                "-c",
                "-ns",
                "-s"
            ).start()
            log.info(
                "PDGF command: {}",
                arrayOf(
                    JAVA_7_DIR,
                    "-jar",
                    PDGF_DIR + File.separator + "pdgf.jar",
                    "-l",
                    tempFile.absolutePath,
                    "-l",
                    PDGF_DIR + File.separator + "config" + File.separator + "customer-output.xml",
                    "-c",
                    "-ns",
                    "-s"
                ) as Any
            )
            log.info(pdgfProcess.info())
            BufferedReader(
                InputStreamReader(
                    pdgfProcess.inputStream,
                    StandardCharsets.UTF_8
                )
            ).use { input ->
                var line: String?
                log.info("PDGF Output:")
                while (input.readLine().also { line = it } != null) {
                    output.append(line).append('\n')
                    log.info(line)
                }
            }
            //wait for process to terminate
            val returnCode = pdgfProcess.waitFor()
            if (returnCode != 0) {
                log.error("PDGF exited with $returnCode")
            }
        } catch (e: IOException) {
            log.error(e)
            return ResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR)
        }
        val endtime = System.currentTimeMillis()
        log.info("---Data Generation finished in " + (endtime - starttime) + " ms.---")
        return ResponseEntity(output.toString(), HttpStatus.OK)
    }

    //will return 500 if exception during test occurs
    @PostMapping(
        path = ["/uploadPDGF/distributed"],
        consumes = [MediaType.APPLICATION_XML_VALUE],
        produces = [MediaType.TEXT_PLAIN_VALUE]
    )
    @Throws(
        InterruptedException::class, ExecutionException::class
    )
    fun uploadDataGenConfigDistributed(@RequestBody pdgfConfig: String?): ResponseEntity<String> {
        val outputs = StringBuffer()
        //data might be outdated by now
        knownOtherInstances.clear()
        //clients can receive request multiple times; retain this message so that nodes that are "late to the party" can still reply
        client.publish(Test.MQTT_TOPIC, IDENTIFICATION_REQUEST_MESSAGE.toByteArray(),1,true)
        sleep(DISCOVERY_TIMEOUT_MS)
        val allNodes = knownOtherInstances.toTypedArray()
        //else the request is rejected
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_XML
        //make sure that we have same order later when Test is run
        allNodes.sort()
        for(i in allNodes.indices){
            var addr = allNodes[i]
            addr = "$addr/uploadPDGF"
            try {
                val request = HttpEntity(pdgfConfig, headers)
                val response = template.postForEntity(URL(addr).toURI(), request, String::class.java)
                if (response.statusCode != HttpStatus.OK) {
                    val reason = "Node $addr responded with code ${response.statusCode}"
                    log.error(reason)
                    outputs.append(reason)
                } else {
                    outputs.append("Node $addr response: "+response.body)
                    continue
                }
            } catch (e:Exception){
                log.error("Could not connect to node $addr ",e)
                outputs.append("Could not connect to node $addr :"+e.message)
            }
        }
        return ResponseEntity(outputs.toString(), HttpStatus.OK)
    }
    //will return 500 if exception during test occurs
    @GetMapping(
        path = ["/exit"]
    )
    fun exit(){
        exitProcess(0)
    }

    fun defaultLocation(): String {
        return "http://localhost:$serverPort";
    }

    companion object {
        private val log = LogManager.getLogger(
            UploadController::class.java
        )
        @JvmField
        var JAVA_7_DIR: String? = null
        @JvmField
        var PDGF_DIR: String? = null

        @JvmField
        var LOCATION: String? = null

        const val IDENTIFICATION_REQUEST_MESSAGE: String = "identify"

        const val IDENTIFICATION_RESPONSE_MESSAGE: String = "identification"

        const val ABORT_MESSAGE: String = "abort"
        @Value("\${discovery.timeout}")
        var DISCOVERY_TIMEOUT_MS:Long = 1000L
    }

    var serverPort = -1;

    override fun onApplicationEvent(event: WebServerInitializedEvent) {
        serverPort = event.webServer.port;
        //no LOCATION was passed as startup parameter. In order to function, use the default one.
        if(LOCATION == null){
            LOCATION = defaultLocation()
            log.warn("No --location argument was passed, using location $LOCATION")
        }
    }
}