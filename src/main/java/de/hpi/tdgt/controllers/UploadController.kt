package de.hpi.tdgt.controllers

import de.hpi.tdgt.concurrency.Event
import de.hpi.tdgt.deserialisation.Deserializer
import de.hpi.tdgt.requesthandling.RestClient
import de.hpi.tdgt.test.Test
import de.hpi.tdgt.test.story.atom.assertion.AssertionStorage
import de.hpi.tdgt.test.time_measurement.TimeStorage
import de.hpi.tdgt.util.PropertiesReader
import kotlinx.coroutines.runBlocking
import org.apache.logging.log4j.LogManager
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.io.*
import java.lang.IllegalArgumentException
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.concurrent.ExecutionException
import java.util.concurrent.Future

@RestController
class UploadController {
    val client:MqttClient
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
                                2,
                                false
                            )
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

    /**
     * In some scenarios, e.g. CLI mode, we need to wait for the thread with the test to terminate.
     */
    var currentThread: Thread? = null
    private fun runTest(testToRun: Test, id: Long) {
        testToRun.testId = id
        runBlocking {
            Event.reset()
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
    @GetMapping(
        path = ["/exit"]
    )
    fun exit(){
        System.exit(0)
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

        @JvmField
        var IDENTIFICATION_REQUEST_MESSAGE: String = "identify"
        @JvmField
        var IDENTIFICATION_RESPONSE_MESSAGE: String = "identification"
    }
}