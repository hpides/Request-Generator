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

public class DeserializeStoryTest {
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
    public void firstStoryHasEightAtoms() throws IOException {
        assertEquals(deserializedTest.getStories()[0].getAtoms().length, 9);
    }

    @Test
    public void secondStoryHasFiveAtoms() throws IOException {
        assertEquals(deserializedTest.getStories()[1].getAtoms().length, 5);
    }

    @Test
    public void cloneCreatesEquivalentStory(){
        val story = deserializedTest.getStories()[0];
        val clone = story.clone();
        val firstAtom = clone.getAtoms()[0];
        assertThat(firstAtom.getSuccessorLinks()[0].getSuccessorLinks()[0].getName(), equalTo("User anlegen"));
    }

    @Test
    public void cloneCreatesNewObject(){
        val story = deserializedTest.getStories()[0];
        val firstAtom = story.getAtoms()[1];
        val clone = firstAtom.clone();
        assertNotSame(clone, firstAtom);
    }

    @Test
    public void cloneCreatesEqualObject(){
        val story = deserializedTest.getStories()[0];
        val clone = story.clone();
        assertThat(clone.getAtoms()[0], equalTo(story.getAtoms()[0]));
    }
    @Test
    public void cloneCreatesNewSuccessorObjects(){
        val story = deserializedTest.getStories()[0];
        val clone = story.clone();
        assertNotSame( clone.getAtoms()[1], story.getAtoms()[1]);
    }
}
