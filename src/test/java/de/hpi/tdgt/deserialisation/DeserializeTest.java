package de.hpi.tdgt.deserialisation;

import lombok.val;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import de.hpi.tdgt.Utils;

import java.io.*;

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

    @Test
    public void correctsScaleFactor() throws IOException{
        val test = Deserializer.deserialize(getExampleJSON());
        val oldScale = test.getScaleFactor();
        test.setNodes(10);
        val newScale = test.getScaleFactor();
        MatcherAssert.assertThat(newScale, Matchers.equalTo(oldScale / 10));
    }



}
