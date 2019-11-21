package de.hpi.tdgt.deserialisation;




import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import de.hpi.tdgt.Utils;

import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

public class DeserializeStory {
    private String getExampleJSON() throws IOException {
        return new Utils().getExampleJSON();
    }

    private de.hpi.tdgt.test.Test deserializedTest;

    @BeforeEach
    public void prepareTest() throws IOException {
        deserializedTest = Deserializer.deserialize(getExampleJSON());
    }

    @Test
    public void hasTwoStories() throws IOException {
        assertEquals(deserializedTest.getStories().length, 2);
    }

    @Test
    public void firstStoryHasEightActivities() throws IOException {
        assertEquals(deserializedTest.getStories()[0].getActivities().length, 8);
    }

    @Test
    public void secondStoryHasFiveActivities() throws IOException {
        assertEquals(deserializedTest.getStories()[1].getActivities().length, 5);
    }

    @Test
    public void cloneCreatesEquivalentStory(){
        val story = deserializedTest.getStories()[0];
        val firstActivity = story.getActivities()[0];
        val clone = firstActivity.clone();
        assertThat(clone.getSuccessors()[0].getName(), equalTo("User anlegen"));
    }

    @Test
    public void cloneCreatesNewObject(){
        val story = deserializedTest.getStories()[0];
        val firstActivity = story.getActivities()[0];
        val clone = firstActivity.clone();
        assertNotSame(clone, firstActivity);
    }

    @Test
    public void cloneCreatesEqualObject(){
        val story = deserializedTest.getStories()[0];
        val firstActivity = story.getActivities()[0];
        val clone = firstActivity.clone();
        assertThat(clone, equalTo(firstActivity));
    }
    @Test
    public void cloneCreatesNewSuccessorObjects(){
        val story = deserializedTest.getStories()[0];
        val firstActivity = story.getActivities()[0];
        val clone = firstActivity.clone();
        assertNotSame( firstActivity.getSuccessors()[0],  clone.getSuccessors()[0]);
    }
}
