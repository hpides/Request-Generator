package deserialisation;

import lombok.val;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import test.story.activity.Data_Generation;

import java.io.*;

import static deserialisation.Utils.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.*;

public class DeserializeTest {
    private String getExampleJSON() throws IOException {
        return new Utils().getExampleJSON();
    }

    @Test
    public void readsClasspathResource() throws IOException {
        Assertions.assertNotEquals("", getExampleJSON());
    }

    @Test
    public void returnsATest() throws IOException {
        Assertions.assertNotNull(Deserializer.deserialize(getExampleJSON()));
    }




}
