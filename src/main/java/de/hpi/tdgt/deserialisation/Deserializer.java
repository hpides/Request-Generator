package de.hpi.tdgt.deserialisation;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.hpi.tdgt.test.Test;
import lombok.val;

import java.io.IOException;

public class Deserializer {
    public static Test deserialize(String json) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        val test = mapper.readValue(json, Test.class);
        test.setConfigJSON(json);
        return test;
    }
}
