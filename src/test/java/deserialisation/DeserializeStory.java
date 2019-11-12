package deserialisation;




import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DeserializeStory {
    private String getExampleJSON() throws IOException {
        return new Utils().getExampleJSON();
    }

    private test.Test deserializedTest;

    @BeforeEach
    public void prepareTest() throws IOException {
        deserializedTest = Deserializer.deserialize(getExampleJSON());
    }

    @Test
    public void hasTwoStories() throws IOException {
        assertEquals(deserializedTest.getStories().length, 2);
    }

    @Test
    public void firstStoryHasSixActivities() throws IOException {
        assertEquals(deserializedTest.getStories()[0].getActivities().length, 6);
    }

    @Test
    public void secondStoryHasTwoActivities() throws IOException {
        assertEquals(deserializedTest.getStories()[1].getActivities().length, 2);
    }
}
