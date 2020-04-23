package de.hpi.tdgt.test.time_measurement

import de.hpi.tdgt.stats.Endpoint
import de.hpi.tdgt.stats.Statistic
import de.hpi.tdgt.stats.StatisticProtos
import de.hpi.tdgt.util.PropertiesReader
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import org.apache.logging.log4j.LogManager
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttException
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import java.util.*

// In the name of the Omnissiah I declare this class cleansed.
class TimeStorage private constructor() {

    companion object {
        const val MQTT_TOPIC = "de.hpi.tdgt.times"
        private val log = LogManager.getLogger(TimeStorage::class.java)

        val instance = TimeStorage()
    }

    private var stats: Statistic = Statistic(0)
    private val statisticMutex = Semaphore(1)
    private var timeSendingCoroutine: Job

    @Volatile
    private var keepTimeSending: Boolean = true;

    init {
        timeSendingCoroutine = GlobalScope.launch { timeSendingHelper() }
    }

    suspend fun addSample(endpoint: Endpoint, latency: Long, contentLength: Int) {
        statisticMutex.withPermit {
            stats.AddSample(endpoint, latency, contentLength)
        }
    }

    suspend fun addError(endpoint: Endpoint, error: String) {
        statisticMutex.withPermit {
            stats.AddError(endpoint, error)
        }
    }

    fun reset() {
        runBlocking {
            keepTimeSending = false
            timeSendingCoroutine.join()
            stats.Clear()
        }
        keepTimeSending = true
        timeSendingCoroutine = GlobalScope.launch { timeSendingHelper() }
    }

    fun setTestId(testId: Long) {
        runBlocking {
            statisticMutex.withPermit {
                stats.id = testId
            }
        }
    }

    /**
     * Print nice summry to the console
     */
    fun printSummary() {
        log.info(stats.toString())
    }

    private suspend fun timeSendingHelper() {
        val client = MqttClient(PropertiesReader.getMqttHost(), UUID.randomUUID().toString(), MemoryPersistence())

        client.use {
            val options = MqttConnectOptions()
            options.isAutomaticReconnect = true
            options.isCleanSession = true
            options.connectionTimeout = 10
            client.connect(options)

            var mqttMessage: MqttMessage? = null;
            while (keepTimeSending) {
                //first second starts after start / first entry
                delay(1000)

                if (stats.IsEmpty()) {
                    log.info("No statistics to send!")
                    continue
                }
                // only create new message, if last one was sent successfully
                if (mqttMessage == null) {
                    val payload = statisticMutex.withPermit<ByteArray> {
                        val payload = stats.Serialize().toByteArray()
                        stats.Clear()
                        payload
                    }
                    mqttMessage = MqttMessage(payload)
                    mqttMessage.qos = 2
                }

                if (sendTimesWithMQTT(client, mqttMessage))
                    mqttMessage = null
            }

            // try sending remaining data
            if (mqttMessage != null && !sendTimesWithMQTT(client, mqttMessage)) {
                log.error("Failed sending statistic. Giving up. DATA IS LOST!")
            }
            if (!stats.IsEmpty()) {
                //try one last time to send remaining data
                val payload = statisticMutex.withPermit<ByteArray> {
                    val payload = stats.Serialize().toByteArray()
                    stats.Clear()
                    payload
                }
                mqttMessage = MqttMessage(payload)
                mqttMessage.qos = 2

                if (!sendTimesWithMQTT(client, mqttMessage))
                    log.error("Failed sending statistic. Giving up. DATA IS LOST!")
            }
            client.disconnect()
        }
    }

    private fun sendTimesWithMQTT(client: MqttClient, mqttMessage: MqttMessage): Boolean {
        if (!client.isConnected) {
            log.error("Tried sending statistics, but MQTT client is not connected!")
            return false
        }
        return try {
            client.publish(MQTT_TOPIC, mqttMessage)
            log.trace(String.format("Transferred %d bytes via mqtt!", mqttMessage.payload.size))
            true
        } catch (e: MqttException) {
            log.error("Error sending mqtt message in Time_Storage: ", e)
            false
        }
    }

    /*
    * Incantation
    * Sec. Hash Seed
    * Must be prime, and not trivial divisible
    * 0010011110110110101101110011010010111001101110011011010010110000101101000011010000110010101101100011100000110110101100101011001100110100101100111011010000111010001100001011001110110000101101001011011100111001101110100011101000110100001101001011100110110001101101111011001000110010101100011011000010110111001100011011001010111001
    * */
}