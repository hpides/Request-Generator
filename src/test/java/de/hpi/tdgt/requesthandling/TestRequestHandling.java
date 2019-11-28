package de.hpi.tdgt.requesthandling;

import de.hpi.tdgt.HttpHandlers;
import de.hpi.tdgt.RequestHandlingFramework;
import de.hpi.tdgt.Utils;
import de.hpi.tdgt.deserialisation.Deserializer;
import de.hpi.tdgt.test.story.UserStory;
import lombok.extern.log4j.Log4j2;
import lombok.val;
import org.junit.jupiter.api.*;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Log4j2
public class TestRequestHandling extends RequestHandlingFramework {


    @Test
    public void testSimpleRequest() throws IOException {
        val rc = new RestClient();
        val result = rc.getFromEndpoint(new URL("http://localhost:9000/"), new HashMap<>());
        String response = new String(result.getResponse(),StandardCharsets.UTF_8);
        assertThat(response, equalTo("Welcome!\n"));
    }

    @Test
    public void testInvalidCharacter() throws IOException {
        val rc = new RestClient();
        val params = new HashMap<String, String>();
        params.put("key","It is what it is...and does what it offers");
        val result = rc.getFromEndpoint(new URL("http://localhost:9000/"), params);
        String response = new String(result.getResponse(),StandardCharsets.UTF_8);
        assertThat(getHandler.getRequest(),equalTo("key=It is what it is...and does what it offers"));
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
    //Regression test
    @Test
    public void testJSONWithInteger() throws IOException, InterruptedException {
        val rq = new de.hpi.tdgt.test.story.activity.Request();
        rq.setAddr("http://localhost:9000/jsonObject");
        rq.setRequestParams(new String[] {"param"});
        rq.setVerb("POST");
        rq.setResponseJSONObject(new String[]{"id"});
        rq.setRepeat(1);
        val params = new HashMap<String,String>();
        params.put("param","value");
        rq.run(params);
        assertThat(rq.getKnownParams(), hasEntry("id","40"));
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
    public void testFirstUserStory() throws IOException, InterruptedException {
        de.hpi.tdgt.test.Test test = Deserializer.deserialize(new Utils().getRequestExampleJSON());
        //do not run second story for this time around; messes with results
        test.setStories(new UserStory[]{test.getStories()[0]});
        test.start();
        Map<String, String> params1 = new HashMap<>(), params2 = new HashMap<>();
        params1.put("key", "wrong");
        params1.put("value","wrong");
        params2.put("key", "user");
        params2.put("value", "pw");
        //should have seen wrong and wrong as well as user and pw
        assertThat("GetHandler should have received key=wrong.",jsonObjectGetHandler.getAllParams().contains(new HttpHandlers.Pair("key", "wrong")), is(true));
        assertThat("GetHandler should have received value=wrong.",jsonObjectGetHandler.getAllParams().contains(new HttpHandlers.Pair("value", "wrong")), is(true));
        assertThat("GetHandler should have received key=user.",jsonObjectGetHandler.getAllParams().contains(new HttpHandlers.Pair("key", "user")), is(true));
        assertThat("GetHandler should have received value=pw.",jsonObjectGetHandler.getAllParams().contains(new HttpHandlers.Pair("value", "pw")), is(true));
        //should have seen wrong and wrong as well as user and pw
        assertThat("GetHandler should have received key=wrong and value=wrong.",postBodyHandler.getAllParameters().contains(params1), is(true));
        assertThat("GetHandler should have received key=user and value=pw.",postBodyHandler.getAllParameters().contains(params2), is(true));
        //repeated 7 times, only one thread has correct data
        assertThat(authHandler.getNumberFailedLogins(), is(6));
    }

    @Test
    public void testUserStoryAgainstTestServer() throws IOException, InterruptedException {
        de.hpi.tdgt.test.Test test = Deserializer.deserialize(new Utils().getRequestExampleJSON());
        test.start();
        //repeated 7 times in first, 3 times 10 times in second story; only first value in corresponding table is correct
        //in one scenario, one instance of first story gets correct param, executes once
        //in other scenario, one instance of second story gets correct param; executes 10 times
        assertThat(authHandler.getNumberFailedLogins(), anyOf(is(36), is(27)));
    }

    @Test
    public void testUserStoryAgainstTestServerWithScaleFactor() throws IOException, InterruptedException {
        de.hpi.tdgt.test.Test test = Deserializer.deserialize(new Utils().getRequestExampleJSON());
        test.start();
        assertThat(jsonObjectGetHandler.getRequestsTotal(), is(7));
    }

    @Test
    public void testUserStoryAgainstTestServerWithScaleFactorAndRepeat() throws IOException, InterruptedException {
        de.hpi.tdgt.test.Test test = Deserializer.deserialize(new Utils().getRequestExampleJSON());
        test.start();
        //10*0.7 times first story executed, calls this once
        //10*0.3 times second story executed, calls this ten times
        assertThat(authHandler.getTotalRequests(), is(7 + 3 * 10));
    }
    @Test
    public void testNoMoreRequestsPerSecondThanSetAreFired() throws InterruptedException, IOException {
        long start = System.currentTimeMillis();
        de.hpi.tdgt.test.Test test = Deserializer.deserialize(new Utils().getRequestExampleJSON());
        //should not take forever
        test.setRequests_per_second(10);
        test.start();
        long end = System.currentTimeMillis();
        int requests_total = handlers.stream().map(HttpHandlers.HttpHandlerBase::getRequests_total).mapToInt(Integer::intValue).sum();
        double duration_seconds = (end - start) / 1000d;
        double requests_per_second = requests_total / duration_seconds;
        log.info("Requests per second: "+requests_per_second);
        assertThat(requests_per_second, lessThanOrEqualTo((double)test.getRequests_per_second()));
    }

}
