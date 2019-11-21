package de.hpi.tdgt.requesthandling;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import de.hpi.tdgt.Utils;
import de.hpi.tdgt.deserialisation.Deserializer;
import de.hpi.tdgt.test.story.activity.Data_Generation;
import lombok.val;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TestRequestHandling {

    private final HttpHandlers.GetHandler getHandler = new HttpHandlers.GetHandler();
    private final HttpHandlers.GetWithBodyHandler getWithBodyHandler = new HttpHandlers.GetWithBodyHandler();
    private final HttpHandlers.JSONObjectGetHandler jsonObjectGetHandler = new HttpHandlers.JSONObjectGetHandler();
    private final HttpHandlers.JSONArrayGetHandler jsonArrayGetHandler = new HttpHandlers.JSONArrayGetHandler();
    private final HttpHandlers.PostHandler postHandler = new HttpHandlers.PostHandler();
    private final HttpHandlers.PostBodyHandler postBodyHandler = new HttpHandlers.PostBodyHandler();
    private final HttpHandlers.PostBodyHandler putBodyHandler = new HttpHandlers.PostBodyHandler();
    private final HttpHandlers.AuthHandler authHandler = new HttpHandlers.AuthHandler();

    //Based on https://www.codeproject.com/tips/1040097/create-a-simple-web-server-in-java-http-server
    @BeforeAll
    public void launchTestServer() throws IOException {
        int port = 9000;
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        System.out.println("server started at " + port);
        server.createContext("/", getHandler);
        server.createContext("/getWithBody", getWithBodyHandler);
        server.createContext("/jsonObject", jsonObjectGetHandler);
        server.createContext("/jsonArray", jsonArrayGetHandler);
        server.createContext("/echoPost", postHandler);
        server.createContext("/postWithBody", postBodyHandler);
        server.createContext("/putWithBody", putBodyHandler);
        server.createContext("/auth", authHandler);
        server.setExecutor(null);
        server.start();

        File values = new File("values.csv");
        values.deleteOnExit();
        var os = new FileOutputStream(values);
        IOUtils.copy(new Utils().getValuesCSV(), os);
        os.close();
    }

    @AfterAll
    public void removeSideEffects(){
        Data_Generation.reset();
    }
    @Test
    public void testSimpleRequest() throws IOException {
        val rc = new RestClient();
        val result = rc.getFromEndpoint(new URL("http://localhost:9000/"), new HashMap<>());
        String response = new String(result.getResponse(),StandardCharsets.UTF_8);
        assertThat(response, equalTo("Welcome!\n"));
    }

    @Test
    public void testContentType() throws IOException {
        val rc = new RestClient();
        val result = rc.getFromEndpoint(new URL("http://localhost:9000/"), new HashMap<>());
        assertThat(result.isPlainText(), is(true));
    }
    @Test
    public void testContentDecoding() throws IOException {
        val rc = new RestClient();
        val result = rc.getFromEndpoint(new URL("http://localhost:9000/"), new HashMap<>());
        assertThat(result.toString(), equalTo("Welcome!\n"));
    }

    @Test
    public void testGETParams() throws IOException {
        val rc = new RestClient();
        val params = new HashMap<String,String>();
        params.put("param","value");
        val result = rc.getFromEndpoint(new URL("http://localhost:9000/"), params);
        assertThat(result.toString(), stringContainsInOrder("param","value"));
    }

    @Test
    public void testGETBodyParams() throws IOException {
        val rc = new RestClient();
        val body = "{\"param\":\"value\"}";
        val result = rc.getBodyFromEndpoint(new URL("http://localhost:9000/getWithBody"), body);
        assertThat(result.toString(), equalTo(body));
    }

    @Test
    public void testDELETEParams() throws IOException {
        val rc = new RestClient();
        val params = new HashMap<String,String>();
        params.put("param","value");
        val result = rc.deleteFromEndpoint(new URL("http://localhost:9000/"), params);
        assertThat(result.toString(), stringContainsInOrder("param","value"));
    }

    @Test
    public void testJSON() throws IOException {
        val rc = new RestClient();
        val params = new HashMap<String,String>();
        params.put("param","value");
        val result = rc.getFromEndpoint(new URL("http://localhost:9000/jsonObject"), params);
        assertThat(result.toJson().isObject(),is(true));
    }
    @Test
    public void testJSONArray() throws IOException {
        val rc = new RestClient();
        val params = new HashMap<String,String>();
        params.put("param","value");
        val result = rc.getFromEndpoint(new URL("http://localhost:9000/jsonArray"), params);
        assertThat(result.toJson().isArray(),is(true));
    }

    @Test
    public void testMeasuresTime() throws IOException {
        val rc = new RestClient();
        val params = new HashMap<String,String>();
        params.put("param","value");
        val result = rc.getFromEndpoint(new URL("http://localhost:9000/jsonArray"), params);
        assertThat(result.durationMillis(), is(both(greaterThan(0L)).and(lessThan(100000L))));
    }

    @Test
    public void testPOSTFormParams() throws IOException {
        val rc = new RestClient();
        val params = new HashMap<String,String>();
        params.put("param","value");
        val result = rc.postFormToEndpoint(new URL("http://localhost:9000/echoPost"), params);
        assertThat(result.toString(), stringContainsInOrder("param = value"));
    }
    @Test
    public void testPUTFormParams() throws IOException {
        val rc = new RestClient();
        val params = new HashMap<String,String>();
        params.put("param","value");
        val result = rc.putFormToEndpoint(new URL("http://localhost:9000/echoPost"), params);
        assertThat(result.toString(), stringContainsInOrder("param = value"));
    }

    @Test
    public void testPOSTBodyParams() throws IOException {
        val rc = new RestClient();
        val body = "{\"param\":\"value\"}";
        val result = rc.postBodyToEndpoint(new URL("http://localhost:9000/postWithBody"), body);
        assertThat(result.toString(), equalTo(body));
    }
    @Test
    public void testPUTBodyParams() throws IOException {
        val rc = new RestClient();
        val body = "{\"param\":\"value\"}";
        val result = rc.putBodyToEndpoint(new URL("http://localhost:9000/postWithBody"), body);
        assertThat(result.toString(), equalTo(body));
    }

    @Test
    public void testGETWithAuth() throws IOException {
        val rc = new RestClient();
        val result = rc.getFromEndpointWithAuth(new URL("http://localhost:9000/auth"),null,HttpHandlers.AuthHandler.username, HttpHandlers.AuthHandler.password);
        assertThat(result.getReturnCode(), equalTo(200));
    }

    @Test
    public void testGETBodyWithAuth() throws IOException {
        val rc = new RestClient();
        val result = rc.getBodyFromEndpointWithAuth(new URL("http://localhost:9000/auth"), "\"Something\"", HttpHandlers.AuthHandler.username, HttpHandlers.AuthHandler.password);
    }

    @Test
    public void testDELETEWithAuth() throws IOException {
        val rc = new RestClient();
        val result = rc.deleteFromEndpointWithAuth(new URL("http://localhost:9000/auth"),null,HttpHandlers.AuthHandler.username, HttpHandlers.AuthHandler.password);
        assertThat(result.getReturnCode(), equalTo(200));
    }
    @Test
    public void testPOSTBodyWithAuth() throws IOException {
        val rc = new RestClient();
        val result = rc.postBodyToEndpointWithAuth(new URL("http://localhost:9000/auth"),"\"Something\"",HttpHandlers.AuthHandler.username, HttpHandlers.AuthHandler.password);
        assertThat(result.getReturnCode(), equalTo(200));
    }
    @Test
    public void testPOSTFormWithAuth() throws IOException {
        val rc = new RestClient();
        val result = rc.postFormToEndpointWithAuth(new URL("http://localhost:9000/auth"),new HashMap<>(),HttpHandlers.AuthHandler.username, HttpHandlers.AuthHandler.password);
        assertThat(result.getReturnCode(), equalTo(200));
    }
    @Test
    public void testPUTBodyWithAuth() throws IOException {
        val rc = new RestClient();
        val result = rc.putBodyToEndpointWithAuth(new URL("http://localhost:9000/auth"),"\"Something\"",HttpHandlers.AuthHandler.username, HttpHandlers.AuthHandler.password);
        assertThat(result.getReturnCode(), equalTo(200));
    }
    @Test
    public void testPUTFormWithAuth() throws IOException {
        val rc = new RestClient();
        val result = rc.putFormToEndpointWithAuth(new URL("http://localhost:9000/auth"),new HashMap<>(),HttpHandlers.AuthHandler.username, HttpHandlers.AuthHandler.password);
        assertThat(result.getReturnCode(), equalTo(200));
    }
    @Test
    public void testUserStoryAgainstTestServer() throws IOException {
        de.hpi.tdgt.test.Test test = Deserializer.deserialize(new Utils().getRequestExampleJSON());
        test.start();
        //assume that "user" and "pw" have been transmitted as form parameters.
        assertThat(postBodyHandler.getLastParameters(), hasEntry("key", HttpHandlers.AuthHandler.username));
        assertThat(postBodyHandler.getLastParameters(), hasEntry("value", HttpHandlers.AuthHandler.password));
        //assume that these parameters have been
        assertThat(jsonObjectGetHandler.getLastParameters(), hasEntry("key", HttpHandlers.AuthHandler.username));
        assertThat(jsonObjectGetHandler.getLastParameters(), hasEntry("value", HttpHandlers.AuthHandler.password));
        //assume  that params have been used correctly in basic auth
        assertThat(authHandler.isLastLoginWasOK(), is(true));
    }

}
