package de.hpi.tdgt.atom;

import de.hpi.tdgt.Utils;
import de.hpi.tdgt.deserialisation.Deserializer;
import de.hpi.tdgt.test.story.atom.Atom;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertNotSame;

public class StartAtomTest {

    private Atom firstAtomOfFirstStory;

    @BeforeEach
    public void prepareTest() throws IOException {
        firstAtomOfFirstStory = Deserializer.deserialize(new Utils().getExampleJSON()).getStories()[0].getAtoms()[0];
    }
    @Test
    public void setFirstAtomOfFirstStoryIsStart(){
        assertNotSame(firstAtomOfFirstStory,firstAtomOfFirstStory.clone());
    }

}
