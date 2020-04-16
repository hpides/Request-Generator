package de.hpi.tdgt.mqtt

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import de.hpi.tdgt.RequestHandlingFramework
import de.hpi.tdgt.Utils
import de.hpi.tdgt.WebApplication
import de.hpi.tdgt.deserialisation.Deserializer.deserialize
import de.hpi.tdgt.test.Test
import de.hpi.tdgt.test.story.UserStory
import de.hpi.tdgt.test.story.atom.Data_Generation
import de.hpi.tdgt.test.story.atom.Request
import de.hpi.tdgt.test.story.atom.assertion.*
import de.hpi.tdgt.test.time_measurement.MqttTimeMessage
import de.hpi.tdgt.test.time_measurement.TimeStorage
import de.hpi.tdgt.util.Pair
import de.hpi.tdgt.util.PropertiesReader
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
import kotlin.collections.HashMap

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
        AssertionStorage.instance.isStoreEntriesAsynch = true
        //scenario that a message we want and a message we don't want arrive at the same time is prevented
        TimeStorage.instance.setSendOnlyNonEmpty(true)
        val mockTest = Test()
        mockParent.parent = mockTest
        mockParent.name = mockStoryName
    }

    @org.junit.jupiter.api.Test
    @Throws(MqttException::class, InterruptedException::class, IOException::class)
    fun TimeStorageStreamsTimesUsingMQTT() = runBlocking {
        val messages: Set<String> = prepareClient(TimeStorage.MQTT_TOPIC)
        TimeStorage.instance.registerTime("POST", "http://localhost:9000/", 10, "story", 0)
        Thread.sleep(3000)
        val typeRef: TypeReference<MqttTimeMessage?> =
            object : TypeReference<MqttTimeMessage?>() {}
        val response = Vector<MqttTimeMessage>()
        for (item in messages) {
            response.add(mapper.readValue(item, typeRef))
        }
        MatcherAssert.assertThat(
            response[0].times,
            Matchers.hasKey("http://localhost:9000/")
        )
    }

    @org.junit.jupiter.api.Test
    @Throws(MqttException::class, InterruptedException::class, IOException::class)
    fun TimeStorageStreamsAllTimesUsingMQTT() = runBlocking  {
        val messages: Set<String> = prepareClient(TimeStorage.MQTT_TOPIC)
        val storyName = "story"
        TimeStorage.instance.registerTime("POST", "http://localhost:9000/", 10, storyName, 0)
        Thread.sleep(3000)
        val typeRef: TypeReference<MqttTimeMessage?> =
            object : TypeReference<MqttTimeMessage?>() {}
        val response = Vector<MqttTimeMessage>()
        for (item in messages) {
            response.add(mapper.readValue(item, typeRef))
        }
        val times =
            response[0].times["http://localhost:9000/"]!!["POST"]!![storyName]
        //key names are typed instead of using the constants to notice if we change it so we can adapt the frontend
        MatcherAssert.assertThat<Set<String>>(
            times!!.keys,
            Matchers.containsInAnyOrder(
                Matchers.equalTo("minLatency"),
                Matchers.equalTo("throughput"),
                Matchers.equalTo("maxLatency"),
                Matchers.equalTo("avgLatency")
            )
        )
    }

    @org.junit.jupiter.api.Test
    @Throws(MqttException::class, InterruptedException::class, IOException::class)
    fun TimeStorageStreamsAllTimesOfAllStoriesUsingMQTT()  = runBlocking { //this test is based on the assumption that both entries are added at roughly the same time, so we want predictable timing behavior
        val messages: Set<String> = prepareClient(TimeStorage.MQTT_TOPIC)
        val storyName1 = "story1"
        val storyName2 = "story2"
        TimeStorage.instance.registerTime("POST", "http://localhost:9000/", 10, storyName1, 0)
        TimeStorage.instance.registerTime("POST", "http://localhost:9000/", 20, storyName2, 0)
        Thread.sleep(3000)
        val typeRef: TypeReference<MqttTimeMessage?> =
            object : TypeReference<MqttTimeMessage?>() {}
        val response = Vector<MqttTimeMessage>()
        for (item in messages) {
            response.add(mapper.readValue(item, typeRef))
        }
        MatcherAssert.assertThat(
            "We should have 2 story entries for \"story1\" and \"story2\"",
            response[0].times["http://localhost:9000/"]!!["POST"]!!.size,
            Matchers.`is`(2)
        )
        val times1 =
            response[0].times["http://localhost:9000/"]!!["POST"]!![storyName1]
        val times2 =
            response[0].times["http://localhost:9000/"]!!["POST"]!![storyName2]
        //key names are typed instead of using the constants to notice if we change it so we can adapt the frontend
        MatcherAssert.assertThat<Set<String>>(
            times1!!.keys,
            Matchers.containsInAnyOrder(
                Matchers.equalTo("minLatency"),
                Matchers.equalTo("throughput"),
                Matchers.equalTo("maxLatency"),
                Matchers.equalTo("avgLatency")
            )
        )
        MatcherAssert.assertThat<Set<String>>(
            times2!!.keys,
            Matchers.containsInAnyOrder(
                Matchers.equalTo("minLatency"),
                Matchers.equalTo("throughput"),
                Matchers.equalTo("maxLatency"),
                Matchers.equalTo("avgLatency")
            )
        )
        MatcherAssert.assertThat<Map<String, String>?>(
            times1,
            Matchers.hasEntry("maxLatency", "10")
        )
        MatcherAssert.assertThat<Map<String, String>?>(
            times2,
            Matchers.hasEntry("maxLatency", "20")
        )
    }

    @org.junit.jupiter.api.Test
    @Throws(
        MqttException::class,
        InterruptedException::class,
        IOException::class,
        ExecutionException::class
    )
    fun TimeStorageStreamsAllTimesUsingMQTTWithCorrectStoryName() {
        val messages: Set<String> = prepareClient(TimeStorage.MQTT_TOPIC)
        val params = HashMap<String, String>()
        params["key"] = "wrong"
        params["value"] = "wrong"
        val getWithAuth =
            deserialize(Utils().requestExampleWithAssertionsJSON).getStories()[0].getAtoms()[3] as Request
        //make sure we do not run successors
        getWithAuth.setSuccessorLinks(arrayOf())
        val name = mockStoryName
        getWithAuth.setParent(mockParent)
        runBlocking{getWithAuth!!.run(params)}
        Thread.sleep(3000)
        val typeRef: TypeReference<MqttTimeMessage?> =
            object : TypeReference<MqttTimeMessage?>() {}
        val response = Vector<MqttTimeMessage>()
        for (item in messages) {
            response.add(mapper.readValue(item, typeRef))
        }
        val times =
            response[0].times["http://localhost:9000/auth"]!!["GET"]
        //key names are typed instead of using the constants to notice if we change it so we can adapt the frontend
        MatcherAssert.assertThat(
            times,
            Matchers.hasKey(name)
        )
    }

    @org.junit.jupiter.api.Test
    fun aggregationOfTimesWorks() {
        val messages: Set<String> = prepareClient(TimeStorage.MQTT_TOPIC)
        val params = HashMap<String, String>()
        params["key"] = "wrong"
        params["value"] = "wrong"
        val test =
            deserialize(Utils().requestExampleWithRequestReplacement)
        //make sure we do not run successors
        test.start()
        Thread.sleep(3000)
        val typeRef: TypeReference<MqttTimeMessage?> =
            object : TypeReference<MqttTimeMessage?>() {}
        val response = Vector<MqttTimeMessage>()
        for (item in messages) {
            response.add(mapper.readValue(item, typeRef))
        }
        //key names are typed instead of using the constants to notice if we change it so we can adapt the frontend
        MatcherAssert.assertThat(
            response[0].times,
            Matchers.hasKey("http://localhost:9000/param=\$value")
        )
        MatcherAssert.assertThat(
            response[0].times["http://localhost:9000/param=\$value"],
            Matchers.hasKey("GET")
        )
    }

    @org.junit.jupiter.api.Test
    fun aggregationOfTimesCanBeDisabled() {
        val messages: Set<String> = prepareClient(TimeStorage.MQTT_TOPIC)
        val params = HashMap<String, String>()
        params["key"] = "wrong"
        params["value"] = "wrong"
        val test =
            deserialize(Utils().requestExampleWithRequestReplacement)
        val request = test.getStories()[0].getAtoms()[1] as Request
        request.timeAggregation = false
        //make sure we do not run successors
        test.start()
        Thread.sleep(3000)
        val typeRef: TypeReference<MqttTimeMessage?> =
            object : TypeReference<MqttTimeMessage?>() {}
        val response = Vector<MqttTimeMessage>()
        for (item in messages) {
            response.add(mapper.readValue(item, typeRef))
        }
        //key names are typed instead of using the constants to notice if we change it so we can adapt the frontend
        MatcherAssert.assertThat(
            response[0].times,
            Matchers.hasKey("http://localhost:9000/param=pw")
        )
        MatcherAssert.assertThat(
            response[0].times["http://localhost:9000/param=pw"],
            Matchers.hasKey("GET")
        )
    }

    @org.junit.jupiter.api.Test
    @Throws(
        MqttException::class,
        InterruptedException::class,
        IOException::class,
        ExecutionException::class
    )
    fun TimeStorageStreamsAllTimesUsingMQTTWithCorrectThroughput() {
        val messages: Set<String> = prepareClient(TimeStorage.MQTT_TOPIC)
        val params = HashMap<String, String>()
        params["key"] = "wrong"
        params["value"] = "wrong"
        val getWithAuth =
            deserialize(Utils().requestExampleWithAssertionsJSON).getStories()[0].getAtoms()[3] as Request
        //make sure we do not run successors
        getWithAuth.setSuccessorLinks(arrayOf())
        val name = mockStoryName
        getWithAuth.setParent(mockParent)
        runBlocking{getWithAuth!!.run(params)}
        Thread.sleep(3000)
        val typeRef: TypeReference<MqttTimeMessage?> =
            object : TypeReference<MqttTimeMessage?>() {}
        val response = Vector<MqttTimeMessage>()
        for (item in messages) {
            response.add(mapper.readValue(item, typeRef))
        }
        val times =
            response[0].times["http://localhost:9000/auth"]!!["GET"]!![name]
        //key names are typed instead of using the constants to notice if we change it so we can adapt the frontend
        MatcherAssert.assertThat(
            times,
            Matchers.hasEntry("throughput", "1")
        )
    }

    @org.junit.jupiter.api.Test
    @Throws(
        MqttException::class,
        InterruptedException::class,
        IOException::class,
        ExecutionException::class
    )
    fun TimeStorageStreamsAllTimesUsingMQTTWithATestId() {
        val messages: Set<String> = prepareClient(TimeStorage.MQTT_TOPIC)
        val params = HashMap<String, String>()
        params["key"] = "wrong"
        params["value"] = "wrong"
        val getWithAuth =
            deserialize(Utils().requestExampleWithAssertionsJSON).getStories()[0].getAtoms()[3] as Request
        //make sure we do not run successors
        getWithAuth.setSuccessorLinks(arrayOf())
        val name = mockStoryName
        getWithAuth.setParent(mockParent)
        runBlocking{getWithAuth!!.run(params)}
        Thread.sleep(3000)
        val typeRef: TypeReference<MqttTimeMessage?> =
            object : TypeReference<MqttTimeMessage?>() {}
        val response = Vector<MqttTimeMessage>()
        for (item in messages) {
            response.add(mapper.readValue(item, typeRef))
        }
        val times = response[0]
        //key names are typed instead of using the constants to notice if we change it so we can adapt the frontend
        MatcherAssert.assertThat(times.testId, Matchers.greaterThan(0L))
    }

    @org.junit.jupiter.api.Test
    @Throws(
        MqttException::class,
        InterruptedException::class,
        IOException::class,
        ExecutionException::class
    )
    fun TimeStorageStreamsAllTimesUsingMQTTWithACreationTime() {
        val messages: Set<String> = prepareClient(TimeStorage.MQTT_TOPIC)
        val params = HashMap<String, String>()
        params["key"] = "wrong"
        params["value"] = "wrong"
        val getWithAuth =
            deserialize(Utils().requestExampleWithAssertionsJSON).getStories()[0].getAtoms()[3] as Request
        //make sure we do not run successors
        getWithAuth.setSuccessorLinks(arrayOf())
        val name = mockStoryName
        getWithAuth.setParent(mockParent)
        runBlocking{getWithAuth!!.run(params)}
        Thread.sleep(3000)
        val typeRef: TypeReference<MqttTimeMessage?> =
            object : TypeReference<MqttTimeMessage?>() {}
        val response = Vector<MqttTimeMessage>()
        for (item in messages) {
            response.add(mapper.readValue(item, typeRef))
        }
        val times = response[0]
        //key names are typed instead of using the constants to notice if we change it so we can adapt the frontend
        MatcherAssert.assertThat(times.creationTime, Matchers.greaterThan(0L))
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
        val msg = MqttMessage(ByteArray(0))
        msg.isRetained = true
        publisher.publish(AssertionStorage.MQTT_TOPIC, msg)
        publisher.publish(TimeStorage.MQTT_TOPIC, msg)
        AssertionStorage.instance.reset()
        publisher.disconnect()
        publisher.close()
    }

    @Throws(IOException::class)
    private fun readAssertion(messages: Set<String>): LinkedList<MqttAssertionMessage> {
        val response = LinkedList<MqttAssertionMessage>()
        //magic to get jackson to serialize to the correct class
        val typeRef: TypeReference<MqttAssertionMessage?> =
            object : TypeReference<MqttAssertionMessage?>() {}
        for (item in messages) {
            try {
                response.add(mapper.readValue(item, typeRef)!!)
            } catch (e: Exception) {
                log.error(e)
            }
        }
        return response
    }

    @org.junit.jupiter.api.Test
    @Throws(
        MqttException::class,
        InterruptedException::class,
        ExecutionException::class,
        IOException::class
    )
    fun ResponseCodeAssertionStreamsFailedAssertions() {
        val params = HashMap<String, String>()
        params["key"] = "wrong"
        params["value"] = "wrong"
        val message: Set<String> = prepareClient(AssertionStorage.MQTT_TOPIC)
        val getWithAuth =
            deserialize(Utils().requestExampleWithAssertionsJSON).getStories()[0].getAtoms()[3] as Request
        //make sure we do not run successors
        getWithAuth.setSuccessorLinks(arrayOf())
        runBlocking{getWithAuth!!.run(params)}
        Thread.sleep(3000)
        val allActuals =
            getAllActuals(message)
        MatcherAssert.assertThat(
            allActuals,
            Matchers.hasItem<Map<String, Pair<Int, Set<String>>?>>(
                Matchers.hasKey("auth does not return 401")
            )
        )
        val actuals = HashSet<String>()
        actuals.add("401")
        MatcherAssert.assertThat(
            allActuals[allActuals.size - 1],
            Matchers.hasEntry<String, Pair<Int, out Set<String>>?>(
                "auth does not return 401",
                Pair(1, actuals)
            )
        )
    }

    @org.junit.jupiter.api.Test
    @Throws(
        MqttException::class,
        InterruptedException::class,
        ExecutionException::class,
        IOException::class
    )
    fun RequestSucceededAssertionStreamsFailedAssertions() {
        val params = HashMap<String, String>()
        params["key"] = "wrong"
        params["value"] = "wrong"
        val message: Set<String> = prepareClient(AssertionStorage.MQTT_TOPIC)
        val getWithAuth =
            deserialize(Utils().requestExampleWithAssertionsJSON).getStories()[0].getAtoms()[3] as Request
        getWithAuth.addr = "http://AHostThatJustCanNotExist"
        //make sure we do not run successors
        getWithAuth.setSuccessorLinks(arrayOf())
        runBlocking{getWithAuth!!.run(params)}
        Thread.sleep(3000)
        val allActuals =
            getAllActuals(message)
        val failedAssertioNName = "Request \"" + getWithAuth.name + "\" is sent"
        MatcherAssert.assertThat(
            allActuals,
            Matchers.hasItem<Map<String, Pair<Int, Set<String>>?>>(
                Matchers.hasKey(failedAssertioNName)
            )
        )
        val actuals = HashSet<String>()
        actuals.add("UnknownHostException:AHostThatJustCanNotExist")
        MatcherAssert.assertThat(
            allActuals[allActuals.size - 1],
            Matchers.hasKey(
                failedAssertioNName
            )
        )
        MatcherAssert.assertThat(
            allActuals[allActuals.size - 1].get(failedAssertioNName)!!.key,
            Matchers.`is`(1)
        )
        //exception message might be localized by the OS, so we can only assert for the first part
        MatcherAssert.assertThat(
            allActuals[allActuals.size - 1].get(failedAssertioNName)!!.value!!.toTypedArray()[0],
            Matchers.containsString("UnknownHostException")
        )
    }

    @org.junit.jupiter.api.Test
    @Throws(
        MqttException::class,
        InterruptedException::class,
        ExecutionException::class,
        IOException::class
    )
    fun ContentTypeAssertionStreamsFailedAssertions() {
        val messages: Set<String> = prepareClient(AssertionStorage.MQTT_TOPIC)
        val params = HashMap<String, String>()
        params["key"] = "something"
        params["value"] = "somethingElse"
        val postWithBodyAndAssertion =
            deserialize(Utils().requestExampleWithAssertionsJSON).getStories()[0].getAtoms()[1] as Request
        //make sure we do not run successors
        postWithBodyAndAssertion.setSuccessorLinks(arrayOf())
        val assertion =
            postWithBodyAndAssertion.assertions[0] as ContentType
        //simulate failure
        assertion.contentType = "application/xml"
        runBlocking{postWithBodyAndAssertion.run(params)}
        Thread.sleep(3000)
        val allActuals =
            getAllActuals(messages)
        MatcherAssert.assertThat(
            allActuals[allActuals.size - 1],
            Matchers.hasKey("postWithBody returns JSON")
        )
        val actuals = HashSet<String>()
        actuals.add("application/json")
        MatcherAssert.assertThat(
            allActuals[allActuals.size - 1],
            Matchers.hasEntry<String, Pair<Int, out Set<String>>?>(
                "postWithBody returns JSON",
                Pair(1, actuals)
            )
        )
    }

    @Throws(IOException::class)
    private fun getAllActuals(messages: Set<String>): LinkedList<Map<String, Pair<Int, MutableSet<String>>>> {
        val allActuals =
            LinkedList<Map<String, Pair<Int, MutableSet<String>>>>()
        for ((_, actuals) in readAssertion(messages)) {
            if (!actuals.isEmpty()) {
                allActuals.add(actuals)
            }
        }
        return allActuals
    }

    @org.junit.jupiter.api.Test
    @Throws(
        MqttException::class,
        InterruptedException::class,
        ExecutionException::class,
        IOException::class
    )
    fun AssertionStorageIsDeletedEverySecond() {
        val messages = prepareClient(AssertionStorage.MQTT_TOPIC)
        val params = HashMap<String, String>()
        params["key"] = "something"
        params["value"] = "somethingElse"
        val postWithBodyAndAssertion =
            deserialize(Utils().requestExampleWithAssertionsJSON).getStories()[0].getAtoms()[1] as Request
        //make sure we do not run successors
        postWithBodyAndAssertion.setSuccessorLinks(arrayOf())
        val assertion =
            postWithBodyAndAssertion.assertions[0] as ContentType
        //simulate failure
        assertion.contentType = "application/xml"
        runBlocking{postWithBodyAndAssertion.run(params)}
        Thread.sleep(3000)
        var allActuals =
            getAllActuals(messages)
        MatcherAssert.assertThat(
            allActuals[allActuals.size - 1],
            Matchers.hasKey("postWithBody returns JSON")
        )
        //remove existing values
        messages.clear()
        assertion.contentType = "application/json"
        runBlocking{postWithBodyAndAssertion.run(params)}
        Thread.sleep(3000)
        //other failure should be removed now
        allActuals = getAllActuals(messages)
        //empty values are filtered
        MatcherAssert.assertThat(
            allActuals,
            Matchers.emptyIterable()
        )
    }

    @org.junit.jupiter.api.Test
    @Throws(
        MqttException::class,
        InterruptedException::class,
        ExecutionException::class,
        IOException::class
    )
    fun AssertionStorageIsDeletedEverySecondIncludingNumberOfActuals() {
        val messages = prepareClient(AssertionStorage.MQTT_TOPIC)
        val params = HashMap<String, String>()
        params["key"] = "something"
        params["value"] = "somethingElse"
        val postWithBodyAndAssertion =
            deserialize(Utils().requestExampleWithAssertionsJSON).getStories()[0].getAtoms()[1] as Request
        //make sure we do not run successors
        postWithBodyAndAssertion.setSuccessorLinks(arrayOf())
        val assertion =
            postWithBodyAndAssertion.assertions[0] as ContentType
        //simulate failure
        assertion.contentType = "application/xml"
        runBlocking{postWithBodyAndAssertion.run(params)}
        Thread.sleep(3000)
        var allActuals =
            getAllActuals(messages)
        MatcherAssert.assertThat(
            allActuals[allActuals.size - 1],
            Matchers.hasKey("postWithBody returns JSON")
        )
        //remove existing values
        messages.clear()
        runBlocking{postWithBodyAndAssertion.run(params)}
        Thread.sleep(3000)
        //other failure should be removed now
        allActuals = getAllActuals(messages)
        //empty values are filtered
        MatcherAssert.assertThat(
            allActuals[allActuals.size - 1],
            Matchers.hasEntry<String, Pair<Int, out Set<String>>?>(
                "postWithBody returns JSON",
                Pair(
                    1,
                    HashSet(listOf("application/json"))
                )
            )
        )
    }

    @org.junit.jupiter.api.Test
    @Throws(
        MqttException::class,
        InterruptedException::class,
        ExecutionException::class,
        IOException::class
    )
    fun ResponseNotEmptyAssertionStreamsFailedAssertions() {
        val params = HashMap<String, String>()
        val messages: Set<String> = prepareClient(AssertionStorage.MQTT_TOPIC)
        val getJsonObjectWithAssertion =
            deserialize(Utils().requestExampleWithAssertionsJSON).getStories()[0].getAtoms()[2] as Request
        //do not run successors
        getJsonObjectWithAssertion.setSuccessorLinks(arrayOf())
        getJsonObjectWithAssertion.addr = "http://localhost:9000/empty"
        runBlocking{getJsonObjectWithAssertion.run(params)}
        Thread.sleep(3000)
        val allActuals =
            getAllActuals(messages)
        MatcherAssert.assertThat(
            allActuals,
            Matchers.hasItem<Map<String, Pair<Int, Set<String>>?>>(
                Matchers.hasKey("jsonObject returns something")
            )
        )
        val actuals = HashSet<String>()
        actuals.add("")
        //hamcrest Matchers.contains did not work, so assume the wanted entry is the last
        MatcherAssert.assertThat(
            allActuals[allActuals.size - 1],
            Matchers.hasEntry<String, Pair<Int, out Set<String>>?>(
                "jsonObject returns something",
                Pair(1, actuals)
            )
        )
    }

    @org.junit.jupiter.api.Test
    @Throws(
        MqttException::class,
        InterruptedException::class,
        ExecutionException::class,
        IOException::class
    )
    fun ResponseNotEmptyAssertionStreamsFailedAssertionsWithTestId() {
        val params = HashMap<String, String>()
        val messages: Set<String> = prepareClient(AssertionStorage.MQTT_TOPIC)
        val getJsonObjectWithAssertion =
            deserialize(Utils().requestExampleWithAssertionsJSON).getStories()[0].getAtoms()[2] as Request
        //do not run successors
        getJsonObjectWithAssertion.setSuccessorLinks(arrayOf())
        getJsonObjectWithAssertion.addr = "http://localhost:9000/empty"
        runBlocking{getJsonObjectWithAssertion.run(params)}
        Thread.sleep(3000)
        val actuals = readAssertion(messages)
        MatcherAssert.assertThat(actuals[0].testId, Matchers.greaterThan(0L))
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

    @org.junit.jupiter.api.Test
    @Throws(
        MqttException::class,
        InterruptedException::class,
        ExecutionException::class,
        IOException::class
    )
    fun AnAssertionErrorIsSentIfDataGenerationDoesNotFindFile() {
        val messages: Set<String> = prepareClient(AssertionStorage.MQTT_TOPIC)
        //test that does not do anything is sufficient, no need to waste resources here
        val generation = Data_Generation()
        generation.table = "NotThere"
        generation.data = arrayOf("NotThere")
        generation.name = "generation"
        runBlocking{generation.perform()}
        val messageStart = "testStart"
        Thread.sleep(3000)
        val actuals = readAssertion(messages)
        MatcherAssert.assertThat(
            actuals[0].actuals,
            Matchers.hasKey("Data Generation \"generation\" has no data remaining")
        )
        val reason: Set<String?> =
            actuals[0].actuals["Data Generation \"generation\" has no data remaining"]!!.value?:HashSet()
        MatcherAssert.assertThat(
            reason,
            Matchers.hasItem(Matchers.containsStringIgnoringCase("NotThere.csv"))
        )
    }

    @org.junit.jupiter.api.Test
    @Throws(
        MqttException::class,
        InterruptedException::class,
        ExecutionException::class,
        IOException::class
    )
    fun AnAssertionErrorIsSentIfDataGenerationHasTooFewLines() {
        val messages: Set<String> = prepareClient(AssertionStorage.MQTT_TOPIC)
        //test that does not do anything is sufficient, no need to waste resources here
        val generation = Data_Generation()
        generation.table = "values"
        generation.data = arrayOf("username", "password")
        generation.name = "generation"
        //file is only 37 long
        generation.repeat = 40
        runBlocking{generation.run(HashMap())}
        val messageStart = "testStart"
        Thread.sleep(3000)
        val actuals = readAssertion(messages)
        var message: MqttAssertionMessage? = null
        for (assertion in actuals) {
            if (!assertion.actuals.isEmpty()) {
                message = assertion
            }
        }
        MatcherAssert.assertThat(
            "Some message should not be without actuals",
            message,
            Matchers.notNullValue()
        )
        MatcherAssert.assertThat(
            message!!.actuals,
            Matchers.hasKey("Data Generation \"generation\" has no data remaining")
        )
        val reason: Set<String?> =
            message.actuals["Data Generation \"generation\" has no data remaining"]!!.value?:HashSet()
        MatcherAssert.assertThat(
            reason,
            Matchers.hasItem(Matchers.containsStringIgnoringCase("read 37 lines from file"))
        )
    }

    @org.junit.jupiter.api.Test
    @Throws(
        MqttException::class,
        InterruptedException::class,
        ExecutionException::class,
        IOException::class
    )
    fun AnAssertionErrorIsSentIfDataGenerationHasTooFewColumns() {
        val messages: Set<String> = prepareClient(AssertionStorage.MQTT_TOPIC)
        //test that does not do anything is sufficient, no need to waste resources here
        val generation = Data_Generation()
        generation.table = "values"
        //only 2 columns in file
        generation.data =
            arrayOf("username", "password", "somethingThatMightJustBeEmpty", "somethingNotExisting")
        generation.name = "generation"
        generation.repeat = 1
        runBlocking{generation.run(HashMap())}
        Thread.sleep(3000)
        val actuals = readAssertion(messages)
        var message: MqttAssertionMessage? = null
        for (assertion in actuals) {
            if (!assertion.actuals.isEmpty()) {
                message = assertion
            }
        }
        MatcherAssert.assertThat(
            "Some message should not be without actuals",
            message,
            Matchers.notNullValue()
        )
        MatcherAssert.assertThat(
            message!!.actuals,
            Matchers.hasKey("Data Generation \"generation\" has too few columns")
        )
        val reason: Set<String?> =
            message.actuals["Data Generation \"generation\" has too few columns"]!!.value?:HashSet()
        MatcherAssert.assertThat(
            reason,
            Matchers.hasItem(Matchers.containsStringIgnoringCase("4 columns requested but only 2 found in file"))
        )
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
        while(startMessage == null){
            startMessage = findMessageStartingWith(message, messageStart)
            sleep(1000)
        }
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

    @org.junit.jupiter.api.Test
    public fun parsesHTMLWithCustomXPATHThrowsAssertionError(){
        val messages: Set<String> = prepareClient(AssertionStorage.MQTT_TOPIC)
        val requestAtom = Request()
        val xpaths = HashMap<String, String>()
        xpaths.put("Bjärk!","user")
        requestAtom.xpaths = xpaths
        requestAtom.extractCSRFTokens(String(Utils().signupHtml.readAllBytes()))
        sleep(2000)
        val actuals = readAssertion(messages)
        var message: MqttAssertionMessage? = null
        for (assertion in actuals) {
            if (!assertion.actuals.isEmpty()) {
                message = assertion
            }
        }
        MatcherAssert.assertThat(message!!.actuals, Matchers.hasKey(Matchers.equalTo("XPATH failed: \"Bjärk!\"")))
        MatcherAssert.assertThat(message.actuals.get("XPATH failed: \"Bjärk!\"")!!.value, Matchers.hasItem(Matchers.containsString("Exception")))
    }

    @org.junit.jupiter.api.Test
    public fun StringReplacementWithUnescapedRelacementThrowsError(){
        val messages: Set<String> = prepareClient(AssertionStorage.MQTT_TOPIC)
        val requestAtom = Request()
        requestAtom.name = "request"
        requestAtom.addr = "http://localhost:9000\$part1\$part2"
        val params = HashMap<String, String>()
        params.put("part1","/")
        requestAtom.predecessorCount = 0
        requestAtom.repeat = 1
        runBlocking { requestAtom.run(params) }
        sleep(2000)
        val actuals = readAssertion(messages)
        var message: MqttAssertionMessage? = null
        for (assertion in actuals) {
            if (!assertion.actuals.isEmpty()) {
                message = assertion
            }
        }
        MatcherAssert.assertThat(message!!.actuals, Matchers.hasKey(Matchers.equalTo("Request request: Could not replace variable(s)  part2")))
        MatcherAssert.assertThat(message.actuals.get("Request request: Could not replace variable(s)  part2")!!.value, Matchers.hasItem(Matchers.containsString("http://localhost:9000/\$part2")))
    }

    @org.junit.jupiter.api.Test
    public fun noAssertionErrorForReplacementIsThrownDuringClone(){
        val messages: Set<String> = prepareClient(AssertionStorage.MQTT_TOPIC)
        val requestAtom = Request()
        requestAtom.name = "request"
        requestAtom.addr = "http://localhost:9000\$part1\$part2"
        requestAtom.predecessorCount = 0
        requestAtom.repeat = 1
        requestAtom.clone()
        sleep(2000)
        val actuals = readAssertion(messages)
        var message: MqttAssertionMessage? = null
        for (assertion in actuals) {
            if (!assertion.actuals.isEmpty()) {
                message = assertion
            }
        }
        MatcherAssert.assertThat(message, Matchers.nullValue())
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
    public fun assertionErrorIsThrownIfXPATHIsNotFound(){
        val messages: Set<String> = prepareClient(AssertionStorage.MQTT_TOPIC)
        val test = de.hpi.tdgt.test.Test()
        val story = UserStory()
        story.parent = test
        val requestAtom = Request()
        requestAtom.setParent(story)
        requestAtom.name = "request"
        requestAtom.verb = "GET"
        requestAtom.addr = "http://localhost:9000/html"
        requestAtom.predecessorCount = 0
        requestAtom.repeat = 1
        val xpathAssertion= XPATHAssertion()
        val xpath = "//h2[text() = 'SomethingThatIsNOTHere']"
        xpathAssertion.xPath = xpath
        xpathAssertion.name = "Has some text"
        requestAtom.assertions = requestAtom.assertions + arrayOf(xpathAssertion)
        runBlocking {requestAtom.run(HashMap())}
        sleep(3000)
        val actuals = readAssertion(messages)
        var message: MqttAssertionMessage? = null
        for (assertion in actuals) {
            if (!assertion.actuals.isEmpty()) {
                message = assertion
            }
        }
        MatcherAssert.assertThat(message!!.actuals, Matchers.hasKey(Matchers.equalTo("Has some text")))
        MatcherAssert.assertThat(message.actuals.get("Has some text")!!.value, Matchers.hasItem(Matchers.equalTo("xpath \"${xpath}\" returned empty result")))
    }
    @org.junit.jupiter.api.Test
    public fun assertionErrorIsThrownIfXPATHIsNotFoundAndPageIsReturned(){
        val messages: Set<String> = prepareClient(AssertionStorage.MQTT_TOPIC)
        val test = de.hpi.tdgt.test.Test()
        val story = UserStory()
        story.parent = test
        val requestAtom = Request()
        requestAtom.setParent(story)
        requestAtom.name = "request"
        requestAtom.verb = "GET"
        requestAtom.addr = "http://localhost:9000/html"
        requestAtom.predecessorCount = 0
        requestAtom.repeat = 1
        val xpathAssertion= XPATHAssertion()
        val xpath = "//h2[text() = 'SomethingThatIsNOTHere']"
        xpathAssertion.xPath = xpath
        xpathAssertion.name = "Has some text"
        xpathAssertion.returnPage = true
        requestAtom.assertions = requestAtom.assertions + arrayOf(xpathAssertion)
        runBlocking {requestAtom.run(HashMap())}
        sleep(3000)
        val actuals = readAssertion(messages)
        var message: MqttAssertionMessage? = null
        for (assertion in actuals) {
            if (!assertion.actuals.isEmpty()) {
                message = assertion
            }
        }
        MatcherAssert.assertThat(message!!.actuals, Matchers.hasKey(Matchers.equalTo("Has some text")))
        MatcherAssert.assertThat(message.actuals.get("Has some text")!!.value, Matchers.hasItem(Matchers.equalTo(String(Utils().signupHtml.readAllBytes()))))
    }

    @org.junit.jupiter.api.Test
    public fun assertionErrorIsThrownIfXPATHIsInvalid(){
        val messages: Set<String> = prepareClient(AssertionStorage.MQTT_TOPIC)
        val test = de.hpi.tdgt.test.Test()
        val story = UserStory()
        story.parent = test
        val requestAtom = Request()
        requestAtom.setParent(story)
        requestAtom.name = "request"
        requestAtom.verb = "GET"
        requestAtom.addr = "http://localhost:9000/html"
        requestAtom.predecessorCount = 0
        requestAtom.repeat = 1
        val xpathAssertion= XPATHAssertion()
        val xpath = "Bjärk!']"
        xpathAssertion.xPath = xpath
        xpathAssertion.name = "Has some text"
        requestAtom.assertions = requestAtom.assertions + arrayOf(xpathAssertion)
        runBlocking {requestAtom.run(HashMap())}
        sleep(3000)
        val actuals = readAssertion(messages)
        var message: MqttAssertionMessage? = null
        for (assertion in actuals) {
            if (!assertion.actuals.isEmpty()) {
                message = assertion
            }
        }
        MatcherAssert.assertThat(message!!.actuals, Matchers.hasKey(Matchers.equalTo("Has some text")))
        MatcherAssert.assertThat(message.actuals.get("Has some text")!!.value, Matchers.hasItem(Matchers.containsString("xpath \"${xpath}\" is invalid")))
    }

    @org.junit.jupiter.api.Test
    public fun noAssertionErrorIsThrownIfXPATHIsFound(){
        val messages: Set<String> = prepareClient(AssertionStorage.MQTT_TOPIC)
        val test = de.hpi.tdgt.test.Test()
        val story = UserStory()
        story.parent = test
        val requestAtom = Request()
        requestAtom.setParent(story)
        requestAtom.name = "request"
        requestAtom.verb = "GET"
        requestAtom.addr = "http://localhost:9000/html"
        requestAtom.predecessorCount = 0
        requestAtom.repeat = 1
        val xpathAssertion= XPATHAssertion()
        xpathAssertion.xPath = "//h2[text() = 'Register New User']"
        requestAtom.assertions = requestAtom.assertions + arrayOf(xpathAssertion)
        runBlocking {requestAtom.run(HashMap())}
        sleep(2000)
        val actuals = readAssertion(messages)
        var message: MqttAssertionMessage? = null
        for (assertion in actuals) {
            if (!assertion.actuals.isEmpty()) {
                message = assertion
            }
        }
        MatcherAssert.assertThat(message, Matchers.nullValue())
    }
    //regression test, this failed in production
    @org.junit.jupiter.api.Test
    public fun noAssertionErrorIsThrownIfXPATHIsFoundAndCorrectlyEscaped(){
        val messages: Set<String> = prepareClient(AssertionStorage.MQTT_TOPIC)
        val test = de.hpi.tdgt.test.Test()
        val story = UserStory()
        story.parent = test
        val requestAtom = Request()
        requestAtom.setParent(story)
        requestAtom.name = "request"
        requestAtom.verb = "GET"
        requestAtom.addr = "http://localhost:9000/html"
        requestAtom.predecessorCount = 0
        requestAtom.repeat = 1
        val xpathAssertion= XPATHAssertion()
        xpathAssertion.xPath = ("//ul[li=\$val]//a/@href")
        requestAtom.assertions = requestAtom.assertions + arrayOf(xpathAssertion)
        var params = HashMap<String, String>()
        params.put("val","I've not done any javascript at all and I' trying to sum up values from the select class. can get both of them displayed, but not summed up. Could anyone explain why I'm getting the \"[object HTMLParagraphElement]\" as the answer? Thank you")
        runBlocking {requestAtom.run(params)}
        sleep(2000)
        val actuals = readAssertion(messages)
        var message: MqttAssertionMessage? = null
        for (assertion in actuals) {
            if (!assertion.actuals.isEmpty()) {
                message = assertion
            }
        }
        MatcherAssert.assertThat(message, Matchers.nullValue())
    }

    @org.junit.jupiter.api.Test
    public fun assertionErrorIsThrownIfJSONPATHIsNotFound(){
        val messages: Set<String> = prepareClient(AssertionStorage.MQTT_TOPIC)
        val test = Test()
        val story = UserStory()
        story.parent = test
        val requestAtom = Request()
        requestAtom.setParent(story)
        requestAtom.name = "request"
        requestAtom.verb = "GET"
        requestAtom.addr = "http://localhost:9000/jsonObject?param=otherValue"
        requestAtom.predecessorCount = 0
        requestAtom.repeat = 1
        val JSONPATHAssertion= JSONPATHAssertion()
        val JSONPATH = "$[?(@.param=~ /value/)]"
        JSONPATHAssertion.JSONPATH = JSONPATH
        JSONPATHAssertion.name = "Has param and value"
        requestAtom.assertions = requestAtom.assertions + arrayOf(JSONPATHAssertion)
        runBlocking {requestAtom.run(HashMap())}
        sleep(3000)
        val actuals = readAssertion(messages)
        var message: MqttAssertionMessage? = null
        for (assertion in actuals) {
            if (!assertion.actuals.isEmpty()) {
                message = assertion
            }
        }
        MatcherAssert.assertThat(message!!.actuals, Matchers.hasKey(Matchers.equalTo(JSONPATHAssertion.name)))
        MatcherAssert.assertThat(message.actuals.get(JSONPATHAssertion.name)!!.value, Matchers.hasItem(Matchers.equalTo("jsonpath \"${JSONPATH}\" returned empty result")))
    }

    @org.junit.jupiter.api.Test
    public fun assertionErrorIsThrownIfJSONPATHIsNotFoundAndReturned(){
        val messages: Set<String> = prepareClient(AssertionStorage.MQTT_TOPIC)
        val test = Test()
        val story = UserStory()
        story.parent = test
        val requestAtom = Request()
        requestAtom.setParent(story)
        requestAtom.name = "request"
        requestAtom.verb = "GET"
        requestAtom.addr = "http://localhost:9000/jsonObject?param=otherValue"
        requestAtom.predecessorCount = 0
        requestAtom.repeat = 1
        val JSONPATHAssertion= JSONPATHAssertion()
        val JSONPATH = "$[?(@.param=~ /value/)]"
        JSONPATHAssertion.JSONPATH = JSONPATH
        JSONPATHAssertion.name = "Has param and value"
        JSONPATHAssertion.returnResponse = true
        requestAtom.assertions = requestAtom.assertions + arrayOf(JSONPATHAssertion)
        runBlocking {requestAtom.run(HashMap())}
        sleep(3000)
        val actuals = readAssertion(messages)
        var message: MqttAssertionMessage? = null
        for (assertion in actuals) {
            if (!assertion.actuals.isEmpty()) {
                message = assertion
            }
        }
        MatcherAssert.assertThat(message!!.actuals, Matchers.hasKey(Matchers.equalTo(JSONPATHAssertion.name)))
        MatcherAssert.assertThat(message.actuals.get(JSONPATHAssertion.name)!!.value, Matchers.hasItem(Matchers.equalTo("{\n\"param\" : \"otherValue\"\n,\"id\" : 40}")))
    }

    @org.junit.jupiter.api.Test
    public fun assertionErrorIsThrownIfJSONPATHIsInvalid(){
        val messages: Set<String> = prepareClient(AssertionStorage.MQTT_TOPIC)
        val test = de.hpi.tdgt.test.Test()
        val story = UserStory()
        story.parent = test
        val requestAtom = Request()
        requestAtom.setParent(story)
        requestAtom.name = "request"
        requestAtom.verb = "GET"
        requestAtom.addr = "http://localhost:9000/jsonObject?param=otherValue"
        requestAtom.predecessorCount = 0
        requestAtom.repeat = 1
        val JSONPATHAssertion= JSONPATHAssertion()
        val JSONPATH = "Bjärk!']"
        JSONPATHAssertion.JSONPATH = JSONPATH
        JSONPATHAssertion.name = "Has some text"
        requestAtom.assertions = requestAtom.assertions + arrayOf(JSONPATHAssertion)
        runBlocking {requestAtom.run(HashMap())}
        sleep(3000)
        val actuals = readAssertion(messages)
        var message: MqttAssertionMessage? = null
        for (assertion in actuals) {
            if (!assertion.actuals.isEmpty()) {
                message = assertion
            }
        }
        MatcherAssert.assertThat(message!!.actuals, Matchers.hasKey(Matchers.equalTo("Has some text")))
        MatcherAssert.assertThat(message.actuals.get("Has some text")!!.value, Matchers.hasItem(Matchers.containsString("jsonpath \"${JSONPATH}\" is invalid")))
    }


    //regression test, this failed in production
    @org.junit.jupiter.api.Test
    public fun noAssertionErrorIsThrownIfJSONPATHIsFoundAndCorrectlyEscaped(){
        val messages: Set<String> = prepareClient(AssertionStorage.MQTT_TOPIC)
        val test = de.hpi.tdgt.test.Test()
        val story = UserStory()
        story.parent = test
        val requestAtom = Request()
        requestAtom.setParent(story)
        requestAtom.name = "request"
        requestAtom.verb = "GET"
        requestAtom.addr = "http://localhost:9000/jsonObject?param=abc'\"//'()"
        requestAtom.predecessorCount = 0
        requestAtom.repeat = 1
        val JSONPATHAssertion= JSONPATHAssertion()
        JSONPATHAssertion.JSONPATH = "$[?(@.\$pName=~ /\$value/)]"
        requestAtom.assertions = requestAtom.assertions + arrayOf(JSONPATHAssertion)
        var params = HashMap<String, String>()
        params.put("value","abc'\"//'()")
        params.put("pName","param")
        runBlocking {requestAtom.run(params)}
        sleep(2000)
        val actuals = readAssertion(messages)
        var message: MqttAssertionMessage? = null
        for (assertion in actuals) {
            if (!assertion.actuals.isEmpty()) {
                message = assertion
            }
        }
        MatcherAssert.assertThat(message, Matchers.nullValue())
    }

    //regression test, this failed in production
    @org.junit.jupiter.api.Test
    public fun assertionErrorIsThrownIfHeaderInvalid(){
        val messages: Set<String> = prepareClient(AssertionStorage.MQTT_TOPIC)
        val test = de.hpi.tdgt.test.Test()
        val story = UserStory()
        story.parent = test
        val requestAtom = Request()
        requestAtom.setParent(story)
        requestAtom.name = "request"
        requestAtom.verb = "GET"
        requestAtom.addr = "http://localhost:9000/"
        requestAtom.predecessorCount = 0
        requestAtom.repeat = 1
        val headers = HashMap<String, String>()
        headers["abc"] = ";;;;"
        requestAtom.sendHeaders = headers
        runBlocking {requestAtom.run(HashMap())}
        sleep(2000)
        val actuals = readAssertion(messages)
        var message: MqttAssertionMessage? = null
        for (assertion in actuals) {
            if (!assertion.actuals.isEmpty()) {
                message = assertion
            }
        }
        MatcherAssert.assertThat(message, Matchers.notNullValue())
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