package de.hpi.tdgt.test.time_measurement

import de.hpi.tdgt.Stats.Endpoint
import de.hpi.tdgt.Stats.Statistic
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
import org.springframework.web.util.HtmlUtils
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean


class TimeStorage protected constructor() {
    //can't be re-connected, create a new instance everytime the thread is run
    private var client: MqttClient? = null
    private var reporter: Thread? = null
    private val mqttReporter: Runnable
    private val running = AtomicBoolean(true)
    private var testID: Long = 0
    var nodeNumber: Long = 0

    /**
     * Flag for tests. If true, only messages that contain times are sent.
     */
    private var sendOnlyNonEmpty = false

    private val sendMutex = Semaphore(1)
    private val entryMutex = Semaphore(1)

    //might be called while client is disconnected by other thread
    private suspend fun sendTimesViaMqtt() = sendMutex.withPermit { //prevent error
        if (stats.IsEmpty())
            return;

        var message = ByteArray(0)
        entryMutex.withPermit {
            message =  toMQTTMessage()
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


    private var stats: Statistic = Statistic();

    private fun toMQTTMessage(): ByteArray {
        //val msg = HtmlUtils.htmlEscape(stats.toString()).toByteArray(StandardCharsets.ISO_8859_1);
        val bytes = stats.Serialize().toByteArray();
        /*val HEX_ARRAY = "0123456789ABCDEF".toCharArray()
        val hexChars = CharArray(bytes.size * 2)
        for (j in bytes.indices) {
            val v: Int = bytes[j].toInt() and 0xFF
            hexChars[j * 2] = HEX_ARRAY.get(v ushr 4)
            hexChars[j * 2 + 1] = HEX_ARRAY.get(v and 0x0F)
        }
        val msgt = String(hexChars);
        val msg = msgt.toByteArray();*/
        //stats.Clear();
        return bytes;
    }

    suspend fun addSample(endpoint: Endpoint, latency: Long, contentLength: Int, story: String?, testid: Long) {
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
                stats.AddSample(endpoint, latency, contentLength)
            }
        }
    }

    suspend fun addError(endpoint: Endpoint, error: String) {
        entryMutex.withPermit {
            stats.AddError(endpoint, error);
        }
    }

    /**
     * Print nice summry to the console
     */
    fun printSummary() {
        log.info(stats.toString());
    }

    fun reset() { //reset might be called twice, so make sure we do not encounter Nullpointer
        if (reporter != null) {
            running.set(false)
            reporter!!.interrupt()
        }
        reporter = null
        stats = Statistic();
    }

    fun setSendOnlyNonEmpty(sendOnlyNonEmpty: Boolean) {
        this.sendOnlyNonEmpty = sendOnlyNonEmpty
    }

    companion object {
        private val log = LogManager.getLogger(TimeStorage::class.java)
        val instance = TimeStorage()

        const val MQTT_TOPIC = "de.hpi.tdgt.times"
    }

    init { //to clean files
        mqttReporter = Runnable {
            try {
                runBlocking { performTimeSending() }
            } catch (e: InterruptedException) {
                //willingly ignored
            }
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