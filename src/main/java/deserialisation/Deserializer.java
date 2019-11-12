package deserialisation;

import com.fasterxml.jackson.databind.ObjectMapper;
import test.Test;

import java.io.IOException;

public class Deserializer {
    public static Test deserialize(String json) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(json, Test.class);
    }
}
