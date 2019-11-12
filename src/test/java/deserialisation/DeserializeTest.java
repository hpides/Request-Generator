package deserialisation;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.*;

public class DeserializeTest {

    private String getExampleJSON() throws IOException {
        StringWriter writer = new StringWriter();
        IOUtils.copy(getClass().getResourceAsStream("test_config_example.json"), writer);
        return writer.toString();
    }

    @Test
    public void readsClasspathResource() throws IOException {
        Assertions.assertNotEquals("", getExampleJSON());
    }

    @Test
    public void returnsATest() throws IOException {
        Assertions.assertNotNull(Deserializer.deserialize(getExampleJSON()));
    }

    @Test
    public void hasTwoStories() throws IOException {
        Assertions.assertEquals(Deserializer.deserialize(getExampleJSON()).getStories().length, 2);
    }

    @Test
    public void firstStoryHasSixActivities() throws IOException {
        Assertions.assertEquals(Deserializer.deserialize(getExampleJSON()).getStories()[0].getActivities().length, 6);
    }
}
