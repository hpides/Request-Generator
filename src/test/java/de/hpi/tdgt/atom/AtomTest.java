package de.hpi.tdgt.atom;

import de.hpi.tdgt.Utils;
import de.hpi.tdgt.deserialisation.Deserializer;
import de.hpi.tdgt.test.story.UserStory;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertSame;


public class AtomTest {
    private UserStory story;
    @BeforeEach
    public void prepare() throws IOException {
        story = Deserializer.deserialize(new Utils().getExampleJSON()).getStories()[0];
    }
    @Test
    public void testCloneCreatesNotTwoDifferentCopiesOfSameObject(){
        val atom = story.getAtoms()[0];
        val clone = atom.clone();
        var successor1 = atom;
        while(!successor1.getName().equals("User löschen story 1")){
            if(successor1.getName().equals("User anlegen")){
                successor1 = successor1.getSuccessorLinks()[1];
            }
            else successor1 = successor1.getSuccessorLinks()[0];
        }

        var successor2 = atom;
        while(!successor2.getName().equals("User löschen story 1")){
            successor2 = successor2.getSuccessorLinks()[0];
        }
        assertSame(successor1,successor2);
    }
}
