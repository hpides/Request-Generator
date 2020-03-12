package de.hpi.tdgt.deserialisation

import com.fasterxml.jackson.databind.ObjectMapper
import de.hpi.tdgt.test.Test
import java.io.IOException

object Deserializer {
    @JvmStatic
    @Throws(IOException::class)
    fun deserialize(json: String?): Test {
        val mapper = ObjectMapper()
        val test =
            mapper.readValue(json, Test::class.java)
        test.configJSON = json
        return test
    }
}