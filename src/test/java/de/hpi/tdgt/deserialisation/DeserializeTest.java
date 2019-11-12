package de.hpi.tdgt.deserialisation;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

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




}
