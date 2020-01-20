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
    public static void assertInstanceOf(Object o, Class c){
        Assertions.assertTrue(c.isInstance(o), "First atom of first story should be a data generation atom and not a "+o.getClass().getName());
    }
    public InputStream getUsersCSV() throws IOException {
        return getClass().getResourceAsStream("users.csv");
    }
    public InputStream getPostsCSV() throws IOException {
        return getClass().getResourceAsStream("posts.csv");
    }

    public InputStream getValuesCSV() throws IOException {
        return getClass().getResourceAsStream("values.csv");
    }
}
