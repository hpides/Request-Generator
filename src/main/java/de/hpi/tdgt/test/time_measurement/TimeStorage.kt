package de.hpi.tdgt.test.time_measurement

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import de.hpi.tdgt.util.PropertiesReader
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import org.apache.logging.log4j.LogManager
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttException
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import java.nio.charset.StandardCharsets
import java.text.NumberFormat
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

class TimeStorage protected constructor() {
    //can't be re-connected, create a new instance everytime the thread is run
    private var client: MqttClient? = null
    private var reporter: Thread? = null
    private val mqttReporter: Runnable
    private val running = AtomicBoolean(true)
    private var testID: Long = 0
    /**
     * Flag for tests. If true, only messages that contain times are sent.
     */
    private var sendOnlyNonEmpty = false

    private val sendMutex = Semaphore(1)
    private val entryMutex = Semaphore(1)

    //might be called while client is disconnected by other thread
    private suspend fun sendTimesViaMqtt() = sendMutex.withPermit{ //prevent error
        var message = ByteArray(0)
        try { //needs to be synchronized so we do not miss entries
            entryMutex.withPermit {
                val entry = toMQTTSummaryMap(registeredTimesLastSecond)
                // tests only want actual times
                if (!sendOnlyNonEmpty || entry.times.isNotEmpty()) {
                    message = mapper.writeValueAsString(entry).toByteArray(StandardCharsets.UTF_8)
                }
                registeredTimesLastSecond.clear()
            }
        } catch (e: JsonProcessingException) {
            log.error(e)
        }
        val mqttMessage = MqttMessage(message)
        //we want to receive every packet EXACTLY once
        mqttMessage.qos = 2
        mqttMessage.isRetained = false
        try {
            client?.publish(MQTT_TOPIC, mqttMessage)
            log.trace(String.format("Transferred %d bytes via mqtt!", message.size))
        } catch (e: MqttException) {
            log.error("Error sending mqtt message in Time_Storage: ", e)
        }
    }

    /**
     * Send all times that might still be stored via mqtt
     */
    suspend fun flush() {
        sendTimesViaMqtt()
    }

    /**
     * Outermost String is the request.
     * Second string from outside is the method.
     * Innermost String is story the request belonged to.
     * TODO remove and replace by real-time aggregation
     */
    private val registeredTimes: MutableMap<String, MutableMap<String, MutableMap<String, MutableList<Long>>>> = HashMap()
    /**
     * Outermost String is the request.
     * Second string from outside is the method.
     * Innermost String is story the request belonged to.
     */
    private val registeredTimesLastSecond: MutableMap<String, MutableMap<String, MutableMap<String, MutableList<Long>>>> = HashMap()

    private fun toMQTTSummaryMap(currentValues: MutableMap<String, MutableMap<String, MutableMap<String, MutableList<Long>>>> = HashMap()): MqttTimeMessage {
        log.trace("Is empty: " + currentValues.isEmpty())
        val ret: MutableMap<String, MutableMap<String, MutableMap<String, MutableMap<String, String>>>> = HashMap()
        //re-create the structure, but using average of the innermost values
        for ((key, value) in currentValues) {
            ret[key] = HashMap()
            for ((key1, value1) in value) {
                ret[key]!![key1] = HashMap()
                for ((key2, value2) in value1) {
                    val avg = value2.stream().mapToLong { obj: Long -> obj }.average().orElse(0.0)
                    val min = value2.stream().mapToLong { obj: Long -> obj }.min().orElse(0)
                    val max = value2.stream().mapToLong { obj: Long -> obj }.max().orElse(0)
                    //number of times this request was sent this second
                    val throughput = value2.size.toLong()
                    val times = HashMap<String, String>()
                    times[THROUGHPUT_STRING] = "" + throughput
                    times[MIN_LATENCY_STRING] = "" + min
                    times[MAX_LATENCY_STRING] = "" + max
                    val nf_out = NumberFormat.getNumberInstance(Locale.UK)
                    nf_out.isGroupingUsed = false
                    times[AVG_LATENCY_STRING] = nf_out.format(avg)
                    ret[key]!![key1]!![key2] = times
                }
            }
        }
        val entry = MqttTimeMessage()
        entry.testId = testID
        entry.creationTime = System.currentTimeMillis()
        entry.times = ret
        return entry
    }

    private val mapper = ObjectMapper()
    suspend fun registerTime(verb: String?, addr: String, latency: Long, story: String?, testid: Long) {
        testID = testid
        if (reporter == null) { //multiple threads might do this simultaneously
            sendMutex.withPermit {
                //test was started after reset was called, so restart the thread
//by now, another thread might have done this already
                if (reporter == null) {
                    reporter = Thread(mqttReporter)
                    log.info("Resumed reporter.")
                    running.set(true)
                    reporter!!.priority = Thread.MAX_PRIORITY
                    reporter!!.start()
                }
            }
        }
        //triggers exception
        if (story != null) {
            entryMutex.withPermit {
                registeredTimes.computeIfAbsent(addr) { HashMap() }
                registeredTimes[addr]!!.computeIfAbsent(verb!!) { HashMap() }
                registeredTimes[addr]!![verb]!!.computeIfAbsent(story) { Vector() }.add(latency)
                registeredTimesLastSecond.computeIfAbsent(addr) { HashMap() }
                registeredTimesLastSecond[addr]!!.computeIfAbsent(verb) { HashMap() }
                registeredTimesLastSecond[addr]!![verb]!!.computeIfAbsent(story) { Vector() }.add(latency)
            }
        }
    }

    /**
     * Times for a certain endpoint.
     * @param verb Like POST, GET, ...
     * @param addr Endpoint
     * @return Array with all times
     */
    fun getTimes(verb: String?, addr: String?): Array<Long?> { //stub
        if (registeredTimes[addr] == null) {
            return arrayOfNulls(0)
        }
        if (registeredTimes[addr]!![verb] == null) {
            return arrayOfNulls(0)
        }
        val allTimes = Vector<Long?>()
        for ((_, value) in registeredTimes[addr]!![verb]!!) {
            allTimes.addAll(value)
        }
        return allTimes.toTypedArray()
    }

    // min, max, avg over the complete run or 0 if can not be computed
    fun getMax(verb: String?, addr: String?): Long {
        val values = getTimes(verb, addr)
        return Arrays.stream(values).mapToLong { obj: Long? -> obj?:0 }.max().orElse(0)
    }

    fun getMin(verb: String?, addr: String?): Long {
        val values = getTimes(verb, addr)
        return Arrays.stream(values).mapToLong { obj: Long? -> obj?:0 }.min().orElse(0)
    }

    fun getAvg(verb: String?, addr: String?): Double {
        val values = getTimes(verb, addr)
        return Arrays.stream(values).mapToLong { obj: Long? -> obj?:0 }.average().orElse(0.0)
    }

    /**
     * Print nice summry to the console
     */
    fun printSummary() {
        for ((key, value) in registeredTimes) {
            for ((key1) in value) {
                log.error("Endpoint " + key1 + " " + key + " min: " + getMin(key1, key) / MS_IN_NS + " ms, max: " + getMax(key1, key) / MS_IN_NS + " ms, avg: " + getAvg(key1, key) / MS_IN_NS + " ms.")
            }
        }
    }

    fun reset() { //reset might be called twice, so make sure we do not encounter Nullpointer
        if (reporter != null) {
            running.set(false)
            reporter!!.interrupt()
        }
        reporter = null
        registeredTimesLastSecond.clear()
        registeredTimes.clear()
    }

    fun setSendOnlyNonEmpty(sendOnlyNonEmpty: Boolean) {
        this.sendOnlyNonEmpty = sendOnlyNonEmpty
    }

    companion object {
        private val log = LogManager.getLogger(TimeStorage::class.java)
        val instance = TimeStorage()

        const val THROUGHPUT_STRING = "throughput"
        const val MIN_LATENCY_STRING = "minLatency"
        const val MAX_LATENCY_STRING = "maxLatency"
        const val AVG_LATENCY_STRING = "avgLatency"
        const val STORY_STRING = "story"
        private const val MS_IN_NS = 1000000.0
        const val MQTT_TOPIC = "de.hpi.tdgt.times"
    }

    init { //to clean files
        mqttReporter = Runnable {
            runBlocking { performTimeSending() }
        }
        reporter = Thread(mqttReporter)
        reporter!!.priority = Thread.MAX_PRIORITY
        reporter!!.start()
    }

    private suspend fun performTimeSending() {
        while (running.get()) { //first second starts after start / first entry
            try {
                delay(1000)
            } catch (e: InterruptedException) { //Clean up
                break
            }
            //client is null if reset was called
            while (client == null || client?.isConnected != true) {
                val publisherId = UUID.randomUUID().toString()
                try {
                    sendMutex.withPermit {
                        //use memory persistence because it is not important that all packets are transferred and we do not want to spam the file system
                        client = MqttClient(PropertiesReader.getMqttHost(), publisherId, MemoryPersistence())
                    }
                } catch (e: MqttException) {
                    log.error("Error creating mqttclient in TimeStorage: ", e)
                    try {
                        delay(1000)
                        continue
                    } catch (ex: InterruptedException) {
                        return
                    }
                }
                val options = MqttConnectOptions()
                options.isAutomaticReconnect = true
                options.isCleanSession = true
                options.connectionTimeout = 10
                try {
                    sendMutex.withPermit {
                        client!!.connect(options)
                        //clear retained messages from last test
                        client!!.publish(MQTT_TOPIC, ByteArray(0), 0, true)
                    }
                } catch (e: MqttException) {
                    log.error("Could not connect to mqtt broker in TimeStorage: ", e)
                    continue
                }
                try {
                    delay(100)
                } catch (e: InterruptedException) {
                    return
                }
            }
            //client is created and connected
            sendTimesViaMqtt()
        }
        //to clean files
        try {
            if (client != null) {
                sendMutex.withPermit {
                    //clear retained messages for next test
                    client?.publish(MQTT_TOPIC, ByteArray(0), 0, true)
                    client?.disconnect()
                    client?.close()
                }
            }
        } catch (e: MqttException) {
            e.printStackTrace()
        }
    }
}