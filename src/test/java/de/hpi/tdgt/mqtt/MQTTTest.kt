package de.hpi.tdgt.mqtt

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import de.hpi.tdgt.RequestHandlingFramework
import de.hpi.tdgt.Utils
import de.hpi.tdgt.deserialisation.Deserializer.deserialize
import de.hpi.tdgt.test.Test
import de.hpi.tdgt.test.story.UserStory
import de.hpi.tdgt.test.story.atom.Request
import de.hpi.tdgt.test.story.atom.assertion.AssertionStorage
import de.hpi.tdgt.test.story.atom.assertion.ContentType
import de.hpi.tdgt.test.story.atom.assertion.MqttAssertionMessage
import de.hpi.tdgt.test.time_measurement.MqttTimeMessage
import de.hpi.tdgt.test.time_measurement.TimeStorage
import de.hpi.tdgt.util.Pair
import de.hpi.tdgt.util.PropertiesReader
import org.apache.logging.log4j.LogManager
import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import java.io.IOException
import java.util.*
import java.util.concurrent.ExecutionException

class MQTTTest : RequestHandlingFramework() {
    private val mapper = ObjectMapper()
    private var publisher: IMqttClient? = null
    private val mockParent = UserStory()
    private val mockStoryName = "StoryName"
    @BeforeEach
    fun beforeEach() { //this test MUST handle asynch behaviour
        AssertionStorage.getInstance().isStoreEntriesAsynch = true
        //scenario that a message we want and a message we don't want arrive at the same time is prevented
        TimeStorage.getInstance().setSendOnlyNonEmpty(true)
        val mockTest = Test()
        mockParent.parent = mockTest
        mockParent.name = mockStoryName
    }

    @org.junit.jupiter.api.Test
    @Throws(MqttException::class, InterruptedException::class, IOException::class)
    fun TimeStorageStreamsTimesUsingMQTT() {
        val messages: Set<String> = prepareClient(TimeStorage.MQTT_TOPIC)
        TimeStorage.getInstance().registerTime("POST", "http://localhost:9000/", 10, "story", 0)
        Thread.sleep(3000)
        val typeRef: TypeReference<MqttTimeMessage> =
            object : TypeReference<MqttTimeMessage>() {}
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
    fun TimeStorageStreamsAllTimesUsingMQTT() {
        val messages: Set<String> = prepareClient(TimeStorage.MQTT_TOPIC)
        val storyName = "story"
        TimeStorage.getInstance().registerTime("POST", "http://localhost:9000/", 10, storyName, 0)
        Thread.sleep(3000)
        val typeRef: TypeReference<MqttTimeMessage> =
            object : TypeReference<MqttTimeMessage>() {}
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
    fun TimeStorageStreamsAllTimesOfAllStoriesUsingMQTT() { //this test is based on the assumption that both entries are added at roughly the same time, so we want predictable timing behavior
        val messages: Set<String> = prepareClient(TimeStorage.MQTT_TOPIC)
        val storyName1 = "story1"
        val storyName2 = "story2"
        TimeStorage.getInstance().registerTime("POST", "http://localhost:9000/", 10, storyName1, 0)
        TimeStorage.getInstance().registerTime("POST", "http://localhost:9000/", 20, storyName2, 0)
        Thread.sleep(3000)
        val typeRef: TypeReference<MqttTimeMessage> =
            object : TypeReference<MqttTimeMessage>() {}
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
            deserialize(Utils().requestExampleWithAssertionsJSON).getStories()[0]!!.atoms[3] as Request
        //make sure we do not run successors
        getWithAuth.setSuccessorLinks(arrayOfNulls(0))
        val name = mockStoryName
        getWithAuth.setParent(mockParent)
        getWithAuth.run(params)
        Thread.sleep(3000)
        val typeRef: TypeReference<MqttTimeMessage> =
            object : TypeReference<MqttTimeMessage>() {}
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
            deserialize(Utils().requestExampleWithAssertionsJSON).getStories()[0]!!.atoms[3] as Request
        //make sure we do not run successors
        getWithAuth.setSuccessorLinks(arrayOfNulls(0))
        val name = mockStoryName
        getWithAuth.setParent(mockParent)
        getWithAuth.run(params)
        Thread.sleep(3000)
        val typeRef: TypeReference<MqttTimeMessage> =
            object : TypeReference<MqttTimeMessage>() {}
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
            deserialize(Utils().requestExampleWithAssertionsJSON).getStories()[0]!!.atoms[3] as Request
        //make sure we do not run successors
        getWithAuth.setSuccessorLinks(arrayOfNulls(0))
        val name = mockStoryName
        getWithAuth.setParent(mockParent)
        getWithAuth.run(params)
        Thread.sleep(3000)
        val typeRef: TypeReference<MqttTimeMessage> =
            object : TypeReference<MqttTimeMessage>() {}
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
            deserialize(Utils().requestExampleWithAssertionsJSON).getStories()[0]!!.atoms[3] as Request
        //make sure we do not run successors
        getWithAuth.setSuccessorLinks(arrayOfNulls(0))
        val name = mockStoryName
        getWithAuth.setParent(mockParent)
        getWithAuth.run(params)
        Thread.sleep(3000)
        val typeRef: TypeReference<MqttTimeMessage> =
            object : TypeReference<MqttTimeMessage>() {}
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
        publisher!!.connect(options)
        val message = HashSet<String>()
        publisher!!.subscribe(topic, IMqttMessageListener { s: String, mqttMessage: MqttMessage ->
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
        publisher!!.publish(AssertionStorage.MQTT_TOPIC, msg)
        publisher!!.publish(TimeStorage.MQTT_TOPIC, msg)
        AssertionStorage.getInstance().reset()
        publisher!!.disconnect()
        publisher!!.close()
    }

    @Throws(IOException::class)
    private fun readAssertion(messages: Set<String>): LinkedList<MqttAssertionMessage> {
        val response = LinkedList<MqttAssertionMessage>()
        //magic to get jackson to serialize to the correct class
        val typeRef: TypeReference<MqttAssertionMessage> =
            object : TypeReference<MqttAssertionMessage>() {}
        for (item in messages) {
            try {
                response.add(mapper.readValue(item, typeRef))
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
            deserialize(Utils().requestExampleWithAssertionsJSON).getStories()[0]!!.atoms[3] as Request
        //make sure we do not run successors
        getWithAuth.setSuccessorLinks(arrayOfNulls(0))
        getWithAuth.run(params)
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
            deserialize(Utils().requestExampleWithAssertionsJSON).getStories()[0]!!.atoms[3] as Request
        getWithAuth.addr = "http://AHostThatJustCanNotExist"
        //make sure we do not run successors
        getWithAuth.setSuccessorLinks(arrayOfNulls(0))
        getWithAuth.run(params)
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
        actuals.add("java.net.UnknownHostException:AHostThatJustCanNotExist")
        MatcherAssert.assertThat(
            allActuals[allActuals.size - 1],
            Matchers.hasEntry<String, Pair<Int, out Set<String>>?>(
                failedAssertioNName,
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
    fun ContentTypeAssertionStreamsFailedAssertions() {
        val messages: Set<String> = prepareClient(AssertionStorage.MQTT_TOPIC)
        val params = HashMap<String, String>()
        params["key"] = "something"
        params["value"] = "somethingElse"
        val postWithBodyAndAssertion =
            deserialize(Utils().requestExampleWithAssertionsJSON).getStories()[0]!!.atoms[1] as Request
        //make sure we do not run successors
        postWithBodyAndAssertion.setSuccessorLinks(arrayOfNulls(0))
        val assertion =
            postWithBodyAndAssertion.assertions[0] as ContentType
        //simulate failure
        assertion.contentType = "application/xml"
        postWithBodyAndAssertion.run(params)
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
    private fun getAllActuals(messages: Set<String>): LinkedList<Map<String, Pair<Int, Set<String>>?>?> {
        val allActuals =
            LinkedList<Map<String, Pair<Int, Set<String>>?>?>()
        for (message in readAssertion(messages)) {
            if (!message.actuals.isEmpty()) {
                allActuals.add(message.actuals)
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
            deserialize(Utils().requestExampleWithAssertionsJSON).getStories()[0]!!.atoms[1] as Request
        //make sure we do not run successors
        postWithBodyAndAssertion.setSuccessorLinks(arrayOfNulls(0))
        val assertion =
            postWithBodyAndAssertion.assertions[0] as ContentType
        //simulate failure
        assertion.contentType = "application/xml"
        postWithBodyAndAssertion.run(params)
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
        postWithBodyAndAssertion.run(params)
        Thread.sleep(3000)
        //other failure should be removed now
        val actuals = HashSet<String>()
        actuals.add("application/json")
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
    fun ResponseNotEmptyAssertionStreamsFailedAssertions() {
        val params = HashMap<String, String>()
        val messages: Set<String> = prepareClient(AssertionStorage.MQTT_TOPIC)
        val getJsonObjectWithAssertion =
            deserialize(Utils().requestExampleWithAssertionsJSON).getStories()[0]!!.atoms[2] as Request
        //do not run successors
        getJsonObjectWithAssertion.setSuccessorLinks(arrayOfNulls(0))
        getJsonObjectWithAssertion.addr = "http://localhost:9000/empty"
        getJsonObjectWithAssertion.run(params)
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
            deserialize(Utils().requestExampleWithAssertionsJSON).getStories()[0]!!.atoms[2] as Request
        //do not run successors
        getJsonObjectWithAssertion.setSuccessorLinks(arrayOfNulls(0))
        getJsonObjectWithAssertion.addr = "http://localhost:9000/empty"
        getJsonObjectWithAssertion.run(params)
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
        val message: Set<String> = prepareClient(Test.MQTT_TOPIC)
        //test that does not do anything is sufficient, no need to waste resources here
        val test =
            deserialize(Utils().noopJson)
        test.start(test.warmup())
        val messageStart = "testStart"
        val hasTestStart = hasMessageStartingWith(message, messageStart)
        MatcherAssert.assertThat("control topic should have received a \"testStart\"!", hasTestStart)
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
        val message: Set<String> = prepareClient(Test.MQTT_TOPIC)
        //test that does not do anything is sufficient, no need to waste resources here
        val test =
            deserialize(Utils().noopJson)
        test.start(test.warmup())
        val messageStart = "testStart"
        val startMessage = findMessageStartingWith(message, messageStart)
        val parts = startMessage!!.split(" ").toTypedArray()
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
        test.start(test.warmup())
        val messageEnd = "testEnd"
        val hasTestEnd = hasMessageStartingWith(messages, messageEnd)
        MatcherAssert.assertThat("control topic should have received a \"testEnd\"!", hasTestEnd)
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