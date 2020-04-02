package de.hpi.tdgt.test.story.atom.assertion

import de.hpi.tdgt.util.Pair

data class MqttAssertionMessage(var testId: Long = 0, var actuals: MutableMap<String, Pair<Int, MutableSet<String>>> = HashMap())