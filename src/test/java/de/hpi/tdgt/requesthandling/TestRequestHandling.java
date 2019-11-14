package de.hpi.tdgt.requesthandling;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import lombok.val;
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

    //Based on https://www.codeproject.com/tips/1040097/create-a-simple-web-server-in-java-http-server
    @BeforeAll
    public void launchTestServer() throws IOException {
        int port = 9000;
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        System.out.println("server started at " + port);
        server.createContext("/", new HttpHandlers.GetHandler());
        server.createContext("/jsonObject", new HttpHandlers.JSONObjectGetHandler());
        server.createContext("/jsonArray", new HttpHandlers.JSONArrayGetHandler());
        server.createContext("/echoPost", new HttpHandlers.PostHandler());
        server.createContext("/postWithBody", new HttpHandlers.PostBodyHandler());
        server.createContext("/putWithBody", new HttpHandlers.PostBodyHandler());
        server.createContext("/auth", new HttpHandlers.AuthHandler());
        server.setExecutor(null);
        server.start();
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


}
