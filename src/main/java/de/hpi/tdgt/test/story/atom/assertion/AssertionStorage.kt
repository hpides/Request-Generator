package de.hpi.tdgt.test.story.atom.assertion

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import de.hpi.tdgt.test.ThreadRecycler
import de.hpi.tdgt.util.Pair
import de.hpi.tdgt.util.PropertiesReader
import org.apache.logging.log4j.LogManager
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttException
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentSkipListSet
import java.util.concurrent.atomic.AtomicBoolean

class AssertionStorage private constructor() {
    private var client: MqttClient? = null
    private var reporter: Thread? = null
    private val mqttRunnable: Runnable
    private val running = AtomicBoolean(true)
    private var testid: Long = 0
    //needs to be synchronized, because concurrently client might be reset
    @Synchronized
    private fun sendCurrentActualsViaMqtt() { //might be called subsequently if reset is called subsequently
        if (client == null || !client!!.isConnected()) {
            return
        }
        //client is created and connected
        var message = ByteArray(0)
        try {
            synchronized(actualsLastSecond) {
                message = mapper.writeValueAsString(
                    MqttAssertionMessage(
                        testid,
                        actualsLastSecond
                    )
                ).toByteArray(StandardCharsets.UTF_8)
                actualsLastSecond.clear()
                log.info("Deleted actuals last second!")
            }
        } catch (e: JsonProcessingException) {
            //log.error(e)
        }
        val mqttMessage = MqttMessage(message)
        //we want to receive every packet EXACTLY Once
        mqttMessage.qos = 2
        mqttMessage.isRetained = false
        try {
            client!!.publish(MQTT_TOPIC, mqttMessage)
            log.info(
                String.format(
                    "Transferred %d bytes via mqtt to $MQTT_TOPIC",
                    message.size
                )
            )
        } catch (e: MqttException) {
            log.error("Error sending mqtt message in Time_Storage: ", e)
        }
    }

    private val actuals: MutableMap<String, Pair<Int, MutableSet<String>>> =
        ConcurrentHashMap()
    //should only be used for tests
    private val actualsLastSecond: MutableMap<String, Pair<Int, MutableSet<String>>> =
        ConcurrentHashMap()

    fun getFails(assertionName: String?): Int {
        return actuals.getOrDefault(
            assertionName,
            Pair<Int, Set<String>>(0, ConcurrentSkipListSet())
        ).key?:0
    }

    @JsonIgnore
    private val mapper = ObjectMapper()
    /**
     * If true, times are stored asynch. Else times are stored synchronously.
     */
    var isStoreEntriesAsynch = true

    fun addFailure(
        assertionName: String,
        actual: String,
        testid: Long
    ) { //we can assume there is just ne test running at any given time, so this is sufficient
        this.testid = testid
        if (isStoreEntriesAsynch) { //needs quite some synchronization time and might run some time, so run it async if possible
            ThreadRecycler.instance.executorService.submit { doAddFailure(assertionName, actual) }
        } else {
            doAddFailure(assertionName, actual)
        }
    }

    private fun doAddFailure(
        assertionName: String,
        actual: String
    ) { //test was started after reset was called, so restart the thread
        if (reporter == null) {
            reporter = Thread(mqttRunnable)
            log.info("Resumed reporter.")
            running.set(true)
            reporter!!.start()
        }
        var pair: Pair<Int, MutableSet<String>>
        synchronized(this) {
            pair = actuals.getOrDefault(
                assertionName,
                Pair(
                    0,
                    ConcurrentSkipListSet()
                )
            )
            actuals.put(assertionName, pair)
        }
        synchronized(actualsLastSecond) {
            pair = actualsLastSecond.getOrDefault(
                assertionName,
                Pair(
                    0,
                    ConcurrentSkipListSet()
                )
            )
            actualsLastSecond.put(assertionName, pair)
        }
        addActual(assertionName, actual)
    }

    /**
     * Send remaining data to subscribers
     */
    fun flush() {
        sendCurrentActualsViaMqtt()
    }

    fun reset() {
        running.set(false)
        //reset might be called twice
        if (reporter != null) {
            reporter!!.interrupt()
        }
        reporter = null
        actuals.clear()
        actualsLastSecond.clear()
        //there were some race conditions if this did not happen, this fixed them
        instance = AssertionStorage()
    }

    /**
     * Prints nice human readable summary to the console
     */
    fun printSummary() {
        for ((key, value) in actuals) {
            log.info("Assertion " + key + " failed " + value.key + " times.")
            if (value.key?:0 > 0) {
                val actuals = StringBuilder("Actual values: [")
                var first = true
                for (actual in this.actuals.getOrDefault(
                    key,
                    Pair(
                        0,
                        ConcurrentSkipListSet()
                    )
                ).value?:HashSet()) {
                    if (!first) {
                        actuals.append(", ")
                    }
                    first = false
                    actuals.append(actual)
                }
                actuals.append("]")
                log.info(actuals)
            }
        }
    }

    /**
     * Store unexpected value.
     * @param assertionName Name of the assertion that failed
     * @param value actual value
     */
    private fun addActual(assertionName: String, value: String) {
        var initialSet = HashSet<String>()
        actuals.putIfAbsent(
            assertionName,
            Pair(0,initialSet)
        )
        actualsLastSecond.putIfAbsent(
            assertionName,
            Pair(0, initialSet)
        )
        var actualValues: MutableSet<String> = actuals[assertionName]!!.value?:HashSet()
        synchronized(actuals) {
            actualValues.add(value)
            //only working way to increment it
            val oldValue = actuals[assertionName]!!.key?:0
            val pair = actuals[assertionName]!!
            pair.key = oldValue + 1
        }
        synchronized(actualsLastSecond) {
            actualValues = actualsLastSecond.getOrDefault(
                assertionName,
                Pair(0, initialSet)
            ).value?:HashSet()
            actualValues.add(value)
            //only working way to increment it
            val oldValue = actualsLastSecond[assertionName]!!.key?:0
            val pair = actualsLastSecond[assertionName]!!
            pair.key = oldValue + 1
        }
    }

    /**
     * Return all unexpected values during the test.
     * @param assertionName Name of he assertion.
     * @return Every value exactly once.
     */
    fun getActual(assertionName: String?): Set<String> {
        return actuals.getOrDefault(
            assertionName,
            Pair<Int, Set<String>>(0, ConcurrentSkipListSet())
        ).value?:HashSet()
    }

    fun getActualsLastSecond(): Map<String, Pair<Int, MutableSet<String>>> {
        return actualsLastSecond
    }

    companion object {
        private val log = LogManager.getLogger(
            AssertionStorage::class.java
        )

        @JvmStatic
        var instance = AssertionStorage()
        const val MQTT_TOPIC = "de.hpi.tdgt.assertions"
    }

    init {
        val publisherId = UUID.randomUUID().toString()
        //we want to receive every packet EXACTLY Once
//to clean files
        mqttRunnable = Runnable {
            //recommended way to make the thread stop
            while (running.get()) { //first second starts after start / first entry
                try {
                    Thread.sleep(1000)
                } catch (e: InterruptedException) {
                    break
                }
                //will be closed in reset, so might have to be re-created here
                while (client == null || !client!!.isConnected()) {
                    try { //do not set null concurrently
                        synchronized(this) {
                            //use memory persistence because it is not important that all packets are transferred and we do not want to spam the file system
                            client = MqttClient(PropertiesReader.getMqttHost(), publisherId, MemoryPersistence())
                        }
                    } catch (e: MqttException) {
                        //log.error("Error creating mqttclient in AssertionStorage: ", e)
                        try {
                            Thread.sleep(1000)
                            continue
                        } catch (ex: InterruptedException) {
                            return@Runnable
                        }
                    }
                    val options = MqttConnectOptions()
                    options.isAutomaticReconnect = true
                    options.isCleanSession = true
                    options.connectionTimeout = 10
                    try {
                        synchronized(this) {
                            client!!.connect(options)
                            //clear retained messages from last test
                            client!!.publish(MQTT_TOPIC, ByteArray(0), 0, true)
                        }
                    } catch (e: MqttException) {
                        /*log.error(
                            "Could not connect to mqtt broker in AssertionStorage: ",
                            e
                        )*/
                        continue
                    }
                }
                sendCurrentActualsViaMqtt()
            }
            //to clean files
            try {
                if (client != null) {
                    synchronized(this) {
                        //clear retained messages for next test
                        client!!.publish(MQTT_TOPIC, ByteArray(0), 0, true)
                        client!!.disconnect()
                        client!!.close()
                        client = null
                        log.info("Cleaned up AssertionStorage!");
                    }
                }
            } catch (e: MqttException) {
                e.printStackTrace()
            }
        }
    }
}