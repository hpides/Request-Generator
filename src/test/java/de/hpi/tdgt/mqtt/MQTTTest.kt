package de.hpi.tdgt.mqtt

import com.fasterxml.jackson.databind.ObjectMapper
import de.hpi.tdgt.RequestHandlingFramework
import de.hpi.tdgt.Utils
import de.hpi.tdgt.WebApplication
import de.hpi.tdgt.controllers.UploadController
import de.hpi.tdgt.deserialisation.Deserializer.deserialize
import de.hpi.tdgt.requesthandling.HttpConstants
import de.hpi.tdgt.requesthandling.Request
import de.hpi.tdgt.stats.Endpoint
import de.hpi.tdgt.stats.StatisticProtos
import de.hpi.tdgt.test.Test
import de.hpi.tdgt.test.story.UserStory
import de.hpi.tdgt.test.story.atom.RequestAtom
import de.hpi.tdgt.test.time_measurement.TimeStorage
import de.hpi.tdgt.util.PropertiesReader
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.apache.logging.log4j.LogManager
import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.http.MediaType
import org.springframework.http.RequestEntity
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.io.IOException
import java.lang.Thread.sleep
import java.net.URL
import java.util.*
import java.util.concurrent.ExecutionException

@ExtendWith(SpringExtension::class)
@SpringBootTest(classes = [WebApplication::class], webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class MQTTTest : RequestHandlingFramework() {
    @LocalServerPort
    private val port = 0

    @Autowired
    private val restTemplate: TestRestTemplate? = null
    private val mapper = ObjectMapper()
    private lateinit var publisher: IMqttClient
    private val mockParent = UserStory()
    private val mockStoryName = "StoryName"

    @BeforeEach
    fun beforeEach() { //this test MUST handle asynch behaviour
        //AssertionStorage.instance.isStoreEntriesAsynch = true
        //scenario that a message we want and a message we don't want arrive at the same time is prevented
        val mockTest = Test()
        mockParent.parent = mockTest
        mockParent.name = mockStoryName
    }


    @org.junit.jupiter.api.Test
    @Throws(MqttException::class, InterruptedException::class, IOException::class)
    fun TimeStorageStreamsTimesUsingMQTT() = runBlocking {
        val messages = prepareTimeClient(TimeStorage.MQTT_TOPIC)
        TimeStorage.instance.setTestId(42)
        TimeStorage.instance.addSample(Endpoint(URL("http://localhost:9000/"), HttpConstants.POST), 10, 1)
        sleep(3000)
        val first = messages[0];
        MatcherAssert.assertThat("Test Id matches", first.id == 42L)
        MatcherAssert.assertThat("Total requests matches", first.total.numRequests == 1)
        MatcherAssert.assertThat("Request endpoint matches", first.populationsList[0].ep.method == StatisticProtos.Endpoint.Method.POST &&
                first.populationsList[0].ep.url.equals("http://localhost:9000/"))
        MatcherAssert.assertThat("Request time matches", first.total.responseTimesList[0].key == 10L)
        MatcherAssert.assertThat("No errors", first.errorsCount == 0)
        MatcherAssert.assertThat("Content length matches", first.total.totalContentLength == 1)
    }

    @org.junit.jupiter.api.Test
    fun TimeStorageStreamsAssertionsUsingMQTT() = runBlocking {

        val messages = prepareTimeClient(TimeStorage.MQTT_TOPIC)
        TimeStorage.instance.setTestId(42)
        TimeStorage.instance.addError(Endpoint(URL("http://localhost:9000/"), HttpConstants.POST), "Railgun is too fast")

        sleep(3000)
        val first = messages[0];
        MatcherAssert.assertThat("Test Id matches", first.id == 42L)
        MatcherAssert.assertThat("Error count matches", first.errorsCount == 1)
    }

    @Throws(MqttException::class)
    private fun prepareTimeClient(topic: String): MutableList<StatisticProtos.Statistic> {
        val publisherId = UUID.randomUUID().toString()
        publisher = MqttClient(PropertiesReader.getMqttHost(), publisherId, MemoryPersistence())
        val options = MqttConnectOptions()
        options.isAutomaticReconnect = true
        options.isCleanSession = true
        options.connectionTimeout = 10
        (publisher as MqttClient).connect(options)
        val message = mutableListOf<StatisticProtos.Statistic>()
        publisher.subscribe(topic, IMqttMessageListener { s: String, mqttMessage: MqttMessage ->
            //hamcrest can't handle empty sets in the list for contains, so filter them out
            if (s == topic && String(mqttMessage.payload) != "{}" && !String(mqttMessage.payload).isEmpty()) {
                log.info("Received " + String(mqttMessage.payload))
                message.add(StatisticProtos.Statistic.parseFrom(mqttMessage.payload))
            }
        })
        return message
    }

    @Throws(MqttException::class)
    private fun prepareClient(topic: String): MutableSet<String> {
        val publisherId = UUID.randomUUID().toString()
        publisher = MqttClient(PropertiesReader.getMqttHost(), publisherId, MemoryPersistence())
        val options = MqttConnectOptions()
        options.isAutomaticReconnect = true
        options.isCleanSession = true
        options.connectionTimeout = 10
        (publisher as MqttClient).connect(options)
        val message = HashSet<String>()
        publisher.subscribe(topic, IMqttMessageListener { s: String, mqttMessage: MqttMessage ->
            //hamcrest can't handle empty sets in the list for contains, so filter them out
            if (s == topic && String(mqttMessage.payload) != "{}" && !String(mqttMessage.payload).isEmpty()) {
                log.info("Received " + String(mqttMessage.payload))
                message.add(String(mqttMessage.payload))
            }
        })
        return message
    }


    @AfterEach
    @Throws(MqttException::class, InterruptedException::class)
    fun closePublisher() { //hack to remove all stored messages
        TimeStorage.instance.reset()
        publisher.disconnect()
        publisher.close()
    }

    @org.junit.jupiter.api.Test
    @Throws(
            MqttException::class,
            InterruptedException::class,
            ExecutionException::class,
            IOException::class
    )
    fun ATestStartMessageIsSent() {
        runBlocking {
            val message: Set<String> = prepareClient(Test.MQTT_TOPIC)
            //test that does not do anything is sufficient, no need to waste resources here
            val test =
                    deserialize(Utils().noopJson)
            test.start(test.warmup())
            val messageStart = "testStart"
            val hasTestStart = hasMessageStartingWith(message, messageStart)
            MatcherAssert.assertThat("control topic should have received a \"testStart\"!", hasTestStart)
        }
    }

    //this test verifies that format expected by performance data storage is met
    @org.junit.jupiter.api.Test
    @Throws(
            MqttException::class,
            InterruptedException::class,
            ExecutionException::class,
            IOException::class
    )
    fun ATestStartMessageWithIdAndConfigIsSent() {
        runBlocking {
            val message: Set<String> = prepareClient(Test.MQTT_TOPIC)
            //test that does not do anything is sufficient, no need to waste resources here
            val test =
                    deserialize(Utils().noopJson)
            test.start(test.warmup())
            val messageStart = "testStart"
            val startMessage = findMessageStartingWith(message, messageStart)
            val parts = startMessage!!.split(" ".toRegex()).toTypedArray()
            //if there are whitespaces in the string, it will be split by them to
            MatcherAssert.assertThat(parts.size, Matchers.greaterThanOrEqualTo(3))
            MatcherAssert.assertThat(
                    parts[0],
                    Matchers.equalTo(messageStart)
            )
            MatcherAssert.assertThat(parts[1].toLong(), Matchers.greaterThan(0L))
            //collect potentially split config
            val sb = StringBuilder()
            var first = true
            for (i in 2 until parts.size) {
                if (!first) {
                    sb.append(' ')
                }
                first = false
                sb.append(parts[i])
            }
            //make sure whitespaces are also preserved
            MatcherAssert.assertThat(
                    sb.toString(),
                    Matchers.equalTo(Utils().noopJson)
            )
        }
    }

    @org.junit.jupiter.api.Test
    @Throws(Exception::class)
    fun runsUserStoryAgainstTestServerSendsTestConfig() {
        val message: Set<String> = prepareClient(Test.MQTT_TOPIC)
        val requestEntity =
                RequestEntity.post(URL("http://localhost:" + port + "/upload/" + System.currentTimeMillis()).toURI())
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(Utils().noopJson)
        restTemplate!!.exchange(requestEntity, String::class.java)
        val messageStart = "testStart"
        var startMessage = findMessageStartingWith(message, messageStart)
        while (startMessage == null) {
            startMessage = findMessageStartingWith(message, messageStart)
            sleep(1000)
        }
        val parts = startMessage.split(" ".toRegex()).toTypedArray()
        //if there are whitespaces in the string, it will be split by them to
        MatcherAssert.assertThat(parts.size, Matchers.greaterThanOrEqualTo(3))
        MatcherAssert.assertThat(
                parts[0],
                Matchers.equalTo(messageStart)
        )
        MatcherAssert.assertThat(parts[1].toLong(), Matchers.greaterThan(0L))
        //collect potentially split config
        val sb = StringBuilder()
        var first = true
        for (i in 2 until parts.size) {
            if (!first) {
                sb.append(' ')
            }
            first = false
            sb.append(parts[i])
        }
        //make sure whitespaces are also preserved
        MatcherAssert.assertThat(
                sb.toString(),
                Matchers.equalTo(Utils().noopJson)
        )
    }

    @org.junit.jupiter.api.Test
    @Throws(
            MqttException::class,
            InterruptedException::class,
            ExecutionException::class,
            IOException::class
    )
    fun ATestEndMessageIsSent() {
        val messages: Set<String> = prepareClient(Test.MQTT_TOPIC)
        //test that does not do anything is sufficient, no need to waste resources here
        val test =
                deserialize(Utils().noopJson)
        runBlocking {
            test.start(test.warmup())
            val messageEnd = "testEnd"
            val hasTestEnd = hasMessageStartingWith(messages, messageEnd)
            MatcherAssert.assertThat("control topic should have received a \"testEnd\"!", hasTestEnd)
        }
    }

    @org.junit.jupiter.api.Test
    @Throws(
            MqttException::class,
            InterruptedException::class,
            ExecutionException::class,
            IOException::class
    )
    fun nodeEndMessagesAndATestEndMessageIsSent() {
        val messages: Set<String> = prepareClient(Test.MQTT_TOPIC)
        //test that does not do anything is sufficient, no need to waste resources here
        val test =
                deserialize(Utils().noopJson)
        test.nodes = 10
        test.nodeNumber = 0
        runBlocking {
            test.start(test.warmup())
            val messageEnd = "nodeEnd 0 ${test.testId}"
            delay(1000)
            val hasNOdeENd = hasMessageStartingWith(messages, messageEnd)
            MatcherAssert.assertThat("control topic should have received a \"nodeEnd\"!", hasNOdeENd)
            for (i in 1..9) {
                publisher.publish(Test.MQTT_TOPIC, "nodeEnd $i ${test.testId}".toByteArray(), 2, false)
            }
            delay(3000)
            val hasTestEnd = hasMessageStartingWith(messages, "testEnd")
            MatcherAssert.assertThat("control topic should have received a \"testEnd\"!", hasTestEnd)
        }
    }

    @org.junit.jupiter.api.Test
    @Throws(
            MqttException::class,
            InterruptedException::class,
            ExecutionException::class,
            IOException::class
    )
    fun NodesRespondWithTheirIdentification() {
        runBlocking {
            val message: Set<String> = prepareClient(Test.MQTT_TOPIC)
            UploadController.LOCATION = "someHost"
            publisher.publish(Test.MQTT_TOPIC, MqttMessage(UploadController.IDENTIFICATION_REQUEST_MESSAGE.toByteArray()))
            //test that does not do anything is sufficient, no need to waste resources here
            delay(3000)
            val hasIDentification = hasMessageStartingWith(message, UploadController.IDENTIFICATION_RESPONSE_MESSAGE)
            MatcherAssert.assertThat("control topic should have received an \"identification\"!", hasIDentification)
        }
    }

    @org.junit.jupiter.api.Test
    @Throws(
            MqttException::class,
            InterruptedException::class,
            ExecutionException::class,
            IOException::class
    )
    fun NodesDoNotRespondWithTheirIdentificationIfNoneKnown() {
        runBlocking {
            val message: Set<String> = prepareClient(Test.MQTT_TOPIC)
            UploadController.LOCATION = null
            publisher.publish(Test.MQTT_TOPIC, MqttMessage(UploadController.IDENTIFICATION_REQUEST_MESSAGE.toByteArray()))
            //test that does not do anything is sufficient, no need to waste resources here
            delay(3000)
            val hasIDentification = hasMessageStartingWith(message, UploadController.IDENTIFICATION_RESPONSE_MESSAGE)
            MatcherAssert.assertThat("control topic should not have received an \"identification\"!", !hasIDentification)
        }
    }

    @org.junit.jupiter.api.Test
    @Throws(IOException::class, InterruptedException::class, ExecutionException::class)
    fun testFirstRequestOfFirstStoryTakesTime() = runBlocking {
        val test =
                deserialize(Utils().requestExampleJSON)
        val messages = prepareTimeClient(TimeStorage.MQTT_TOPIC)

        //do not run second story for this time around; messes with results
        test.setStories(arrayOf(test.getStories()[0]))
        test.start()

        delay(2000)
        val first = messages[0]
        MatcherAssert.assertThat(
                first.total.numRequests,
                Matchers.greaterThan(0)
        )
    }

    @org.junit.jupiter.api.Test
    @Throws(IOException::class, InterruptedException::class, ExecutionException::class)
    fun testFirstRequestOfFirstStoryHasMaxTimeOverNull() = runBlocking {
        val test =
                deserialize(Utils().requestExampleJSON)
        val messages = prepareTimeClient(TimeStorage.MQTT_TOPIC)
        //do not run second story for this time around; messes with results
        test.setStories(arrayOf(test.getStories()[0]))
        test.start()

        delay(2000)
        val first = messages[0]
        MatcherAssert.assertThat(
                first.total.maxResponseTime,
                Matchers.greaterThan(0L)
        )
    }

    @org.junit.jupiter.api.Test
    @Throws(IOException::class, InterruptedException::class, ExecutionException::class)
    fun testFirstRequestOfFirstStoryHasMinTimeOverNull() = runBlocking {
        val test =
                deserialize(Utils().requestExampleJSON)
        val messages = prepareTimeClient(TimeStorage.MQTT_TOPIC)
        //do not run second story for this time around; messes with results
        test.setStories(arrayOf(test.getStories()[0]))
        test.start()
        delay(2000)
        val first = messages[0]
        MatcherAssert.assertThat(
                first.total.minResponseTime,
                Matchers.greaterThan(0L)
        )
    }

    @org.junit.jupiter.api.Test
    @Throws(IOException::class, InterruptedException::class, ExecutionException::class)
    fun testFirstRequestOfFirstStoryHasTotalTimeOverNull() = runBlocking {
        val test =
                deserialize(Utils().requestExampleJSON)
        val messages = prepareTimeClient(TimeStorage.MQTT_TOPIC)
        //do not run second story for this time around; messes with results
        test.setStories(arrayOf(test.getStories()[0]))
        test.start()
        delay(2000)
        val first = messages[0]
        MatcherAssert.assertThat(first.total.totalResponseTime, Matchers.greaterThan(0L)
        )
    }


    private fun findMessageStartingWith(
            messages: Set<String>,
            messageStart: String
    ): String? {
        for (message in messages) {
            if (message.startsWith(messageStart)) {
                return message
            }
        }
        return null
    }

    private fun hasMessageStartingWith(
            messages: Set<String>,
            messageStart: String
    ): Boolean {
        return findMessageStartingWith(messages, messageStart) != null
    }

    companion object {
        private val log =
                LogManager.getLogger(MQTTTest::class.java)
    }
}