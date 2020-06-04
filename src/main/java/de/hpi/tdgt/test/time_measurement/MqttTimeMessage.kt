package de.hpi.tdgt.test.time_measurement

import java.util.concurrent.ConcurrentHashMap

class MqttTimeMessage {
    var times: Map<String, Map<String, Map<String, Map<String, String>>>> = ConcurrentHashMap()
    var testId: Long = 0
    var creationTime: Long = 0
    var node: String = "unknown node"
}