package deserialisation;




import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import test.story.activity.Activity;
import test.story.activity.Data_Generation;
import test.story.activity.Request;

import java.io.IOException;
import java.util.Arrays;
import java.util.Vector;

import static deserialisation.Utils.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DeserializeActivity {
    private String getExampleJSON() throws IOException {
        return new Utils().getExampleJSON();
    }

    private Activity firstActivityOfFirstStory;
    private Activity secondActivityOfFirstStory;
    private Activity secondActivityOfSecondStory;

    @BeforeEach
    public void prepareTest() throws IOException {
        firstActivityOfFirstStory = Deserializer.deserialize(getExampleJSON()).getStories()[0].getActivities()[0];
        secondActivityOfFirstStory = Deserializer.deserialize(getExampleJSON()).getStories()[0].getActivities()[1];
        secondActivityOfSecondStory = Deserializer.deserialize(getExampleJSON()).getStories()[1].getActivities()[1];
    }

    @Test
    public void firstActivityOfFirstStoryIsDataGeneration() {
        assertInstanceOf(firstActivityOfFirstStory, Data_Generation.class);
    }

    @Test
    public void secondActivityOfSecondStoryIsRequest() {
        assertInstanceOf(secondActivityOfSecondStory, Request.class);
    }

    @Test
    public void firstActivityOfFirstStoryGetsUsernameAndPasswordFromUsers() throws IOException {
        val firstActivityOfFirstStory = (Data_Generation) this.firstActivityOfFirstStory;
        assertArrayEquals(new String[]{"username","password"},firstActivityOfFirstStory.getData());
        assertEquals("users", firstActivityOfFirstStory.getTable());
    }
    @Test
    public void secondActivityOfSecondStorySendsGETRequest(){
        val secondActivityOfSecondStory = (Request) this.secondActivityOfSecondStory;
        assertEquals("GET", secondActivityOfSecondStory.getVerb());
    }

    @Test
    public void secondActivityOfSecondStoryHasCorrectAddress(){
        val secondActivityOfSecondStory = (Request) this.secondActivityOfSecondStory;
        assertEquals("http://search/posts/search", secondActivityOfSecondStory.getAddr());
    }

    @Test
    public void secondActivityOfSecondStoryHasCorrectRequestParams(){
        val secondActivityOfSecondStory = (Request) this.secondActivityOfSecondStory;
        assertArrayEquals(new String[]{"key"}, secondActivityOfSecondStory.getRequestParams());
    }

    @Test
    public void secondActivityOfSecondStoryHasCorrectResponseParams(){
        val secondActivityOfSecondStory = (Request) this.secondActivityOfSecondStory;
        assertArrayEquals(new String[0], secondActivityOfSecondStory.getResponseParams());
    }
    @Test
    public void secondActivityOfSecondStoryHasNoSuccessors(){
        assertArrayEquals(new Activity[0], secondActivityOfSecondStory.getSuccessors());
    }
    @Test
    public void firstActivityOfFirstStoryHasOneSuccessor(){
        assertEquals(1,firstActivityOfFirstStory.getSuccessors().length);
    }
    @Test
    public void firstActivityOfFirstStoryHasCorrectSuccessor(){
        assertEquals(1,firstActivityOfFirstStory.getSuccessors()[0].getId());
    }
    @Test
    public void secondActivityOfFirstStoryHasCorrectSuccessors(){
        val successors = new Vector<Integer>();
        //no implicit parallelism here, we need the exact order for the assert
        for(Activity successor : secondActivityOfFirstStory.getSuccessors()){
            successors.add(successor.getId());
        }
        assertArrayEquals(new Integer[]{2,4}, successors.toArray(new Integer[0]));
    }
}
