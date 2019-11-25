package de.hpi.tdgt.activities;

import de.hpi.tdgt.Utils;
import de.hpi.tdgt.deserialisation.Deserializer;
import de.hpi.tdgt.test.story.UserStory;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertSame;


public class ActivityTest {
    private UserStory story;
    @BeforeEach
    public void prepare() throws IOException {
        story = Deserializer.deserialize(new Utils().getExampleJSON()).getStories()[0];
    }
    @Test
    public void testCloneCreatesNotTwoDifferentCopiesOfSameObject(){
        val activity = story.getActivities()[0];
        val clone = activity.clone();
        var successor1 = activity;
        while(!successor1.getName().equals("User löschen story 1")){
            if(successor1.getName().equals("User anlegen")){
                successor1 = successor1.getSuccessors()[1];
            }
            else successor1 = successor1.getSuccessors()[0];
        }

        var successor2 = activity;
        while(!successor2.getName().equals("User löschen story 1")){
            successor2 = successor2.getSuccessors()[0];
        }
        assertSame(successor1,successor2);
    }
}
