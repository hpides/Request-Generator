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



    @Test
    public void correctsConcurrentRequests() throws IOException{
        val test = Deserializer.deserialize(getExampleJSON());
        val oldScale = test.getMaximumConcurrentRequests();
        test.setNodes(10);
        val newScale = test.getMaximumConcurrentRequests();
        MatcherAssert.assertThat(newScale, Matchers.equalTo(oldScale / 10));
    }

    @Test
    public void correctsInstancesPerSecond() throws IOException{
        val test = Deserializer.deserialize(getExampleJSON());
        val oldScale = test.getActiveInstancesPerSecond();
        test.setNodes(10);
        val newScale = test.getActiveInstancesPerSecond();
        MatcherAssert.assertThat(newScale, Matchers.equalTo(oldScale / 10));
    }

    @Test
    public void correctsScaleFactorNotToZero() throws IOException{
        val test = Deserializer.deserialize(getExampleJSON());
        val oldScale = test.getScaleFactor();
        test.setNodes(Integer.MAX_VALUE);
        val newScale = test.getScaleFactor();
        MatcherAssert.assertThat(newScale, Matchers.greaterThan(0L));
    }

    @Test
    public void correctsMaxConcurrentRequestsNotToZero() throws IOException{
        val test = Deserializer.deserialize(getExampleJSON());
        val oldScale = test.getMaximumConcurrentRequests();
        test.setNodes(Integer.MAX_VALUE);
        val newScale = test.getMaximumConcurrentRequests();
        MatcherAssert.assertThat(newScale, Matchers.greaterThan(0));
    }

    @Test
    public void correctsActiveInstancesNotToZero() throws IOException{
        val test = Deserializer.deserialize(getExampleJSON());
        val oldScale = test.getActiveInstancesPerSecond();
        test.setNodes(Integer.MAX_VALUE);
        val newScale = test.getActiveInstancesPerSecond();
        MatcherAssert.assertThat(newScale, Matchers.greaterThan(0));
    }

    @Test
    public void hasCorrectName() throws IOException{
        val test = Deserializer.deserialize(getExampleJSON());
        MatcherAssert.assertThat(test.getName(), Matchers.equalTo("An example test"));
    }

}
