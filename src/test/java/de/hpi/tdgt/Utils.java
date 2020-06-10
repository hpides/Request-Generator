package de.hpi.tdgt;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Assertions;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;

public class Utils {
    public String getExampleJSON() throws IOException {
        StringWriter writer = new StringWriter();
        IOUtils.copy(getClass().getResourceAsStream("test_config_example.json"), writer);
        return writer.toString();
    }
    public String getNoopJson() throws IOException {
        StringWriter writer = new StringWriter();
        IOUtils.copy(getClass().getResourceAsStream("NoopTest.json"), writer);
        return writer.toString();
    }

    public String getRequestExampleJSON() throws IOException {
        StringWriter writer = new StringWriter();
        IOUtils.copy(getClass().getResourceAsStream("RequestExample.json"), writer);
        return writer.toString();
    }

    public String getRequestExampleJSONWithDelay() throws IOException {
        StringWriter writer = new StringWriter();
        IOUtils.copy(getClass().getResourceAsStream("RequestExampleWithDelay.json"), writer);
        return writer.toString();
    }

    public String getRequestExampleWithRepeatJSON() throws IOException {
        StringWriter writer = new StringWriter();
        IOUtils.copy(getClass().getResourceAsStream("RequestExampleWithRepeat.json"), writer);
        return writer.toString();
    }
    public String getRequestExampleWithManyParallelRequests() throws IOException {
        StringWriter writer = new StringWriter();
        IOUtils.copy(getClass().getResourceAsStream("RequestExampleWithManyParallelRequests.json"), writer);
        return writer.toString();
    }

    public String getRequestExampleWithNonIndexIDsJSON() throws IOException {
        StringWriter writer = new StringWriter();
        IOUtils.copy(getClass().getResourceAsStream("RequestExampleWithNonIndexIDs.json"), writer);
        return writer.toString();
    }

    public String getRequestExampleWithAssertionsJSON() throws IOException {
        StringWriter writer = new StringWriter();
        IOUtils.copy(getClass().getResourceAsStream("RequestExampleWithAssertions.json"), writer);
        return writer.toString();
    }
    public String getRequestExampleWithAssertionsAndWarmupJSON() throws IOException {
        StringWriter writer = new StringWriter();
        IOUtils.copy(getClass().getResourceAsStream("RequestExampleWithAssertionsAndWarmup.json"), writer);
        return writer.toString();
    }
    public String getRequestExampleWithTokens() throws IOException {
        StringWriter writer = new StringWriter();
        IOUtils.copy(getClass().getResourceAsStream("RequestExampleWithTokens.json"), writer);
        return writer.toString();
    }
    public String getRequestExampleWithRequestReplacement() throws IOException {
        StringWriter writer = new StringWriter();
        IOUtils.copy(getClass().getResourceAsStream("RequestExampleWithRequestReplacement.json"), writer);
        return writer.toString();
    }
    public static void assertInstanceOf(Object o, Class c){
        Assertions.assertTrue(c.isInstance(o), "First atom of first story should be a data generation atom and not a "+o.getClass().getName());
    }
    public InputStream getUsersCSV() {
        return getClass().getResourceAsStream("output/users.csv");
    }


    public String getAssignmentExample() throws IOException {
        StringWriter writer = new StringWriter();
        IOUtils.copy(getClass().getResourceAsStream("assignment.json"), writer);
        return writer.toString();
    }
    public InputStream getPostsCSV() {
        return getClass().getResourceAsStream("output/posts.csv");
    }

    public InputStream getValuesCSV() {
        return getClass().getResourceAsStream("output/values.csv");
    }

    public InputStream getSignupHtml() {
        return getClass().getResourceAsStream("signup.html");
    }
}
