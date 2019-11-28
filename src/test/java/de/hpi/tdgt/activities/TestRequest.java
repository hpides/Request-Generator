package de.hpi.tdgt.activities;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertNotSame;

import de.hpi.tdgt.HttpHandlers;
import de.hpi.tdgt.RequestHandlingFramework;
import de.hpi.tdgt.Utils;
import de.hpi.tdgt.deserialisation.Deserializer;
import de.hpi.tdgt.test.story.activity.Request;
import de.hpi.tdgt.test.story.activity.assertion.AssertionStorage;
import de.hpi.tdgt.test.story.activity.assertion.ContentType;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import lombok.val;

import java.io.IOException;
import java.util.HashMap;

public class TestRequest extends RequestHandlingFramework {

    private Request requestActivity;
    private Request postWithBodyAndAssertion;
    private Request getJsonObjectWithAssertion;
    private Request getWithAuth;
    @BeforeEach
    public void prepareTest() throws IOException {
        requestActivity = new Request();
        requestActivity.setVerb("GET");
        requestActivity.setAddr("http://example.com");
        postWithBodyAndAssertion = (Request) Deserializer.deserialize(new Utils().getRequestExampleWithAssertionsJSON()).getStories()[0].getActivities()[1];
        getJsonObjectWithAssertion = (Request) Deserializer.deserialize(new Utils().getRequestExampleWithAssertionsJSON()).getStories()[0].getActivities()[2];
        getWithAuth = (Request) Deserializer.deserialize(new Utils().getRequestExampleWithAssertionsJSON()).getStories()[0].getActivities()[3];
    }
    @AfterEach
    public void clearAssertions(){
        AssertionStorage.getInstance().reset();
    }

    @Test
    public void cloneCreatesEquivalentObject() {
        val clone = requestActivity.clone();
        assertThat(clone, equalTo(requestActivity));
    }

    @Test
    public void cloneCreatesEquivalentObjectWhenAllAttribvutesAreSet() {
        requestActivity.setResponseJSONObject(new String[]{"item1", "item2"});
        //noch 10
        requestActivity.setResponseParams(new String[]{"item3", "item4"});
        requestActivity.setRequestJSONObject(new String[]{"item5", "item6"});
        requestActivity.setRequestParams(new String[]{"item7", "item8"});
        requestActivity.setBasicAuth(new Request.BasicAuth("user","pw"));
        requestActivity.setId(0);
        requestActivity.setName("Some Request");
        requestActivity.setPredecessorCount(1);
        requestActivity.setRepeat(3);
        requestActivity.setSuccessors(new int[0]);

        val clone = requestActivity.clone();
        //would have been set by story
        clone.setPredecessorCount(1);
        assertThat(clone, equalTo(requestActivity));
    }

    @Test
    public void cloneCreatesotherObject() {
        val clone = requestActivity.clone();
        assertNotSame(clone, requestActivity);
    }
    @Test
    public void ContentTypeAssertNotFailingIfCorrect() throws InterruptedException {
        val params = new HashMap<String, String>();
        params.put("key","something");
        params.put("value","somethingElse");
        postWithBodyAndAssertion.run(params);
        assertThat(AssertionStorage.getInstance().getFails("postWithBody returns JSON"), Matchers.is(0));
    }

    @Test
    public void ContentTypeAssertFailingIfFalse() throws InterruptedException {
        val params = new HashMap<String, String>();
        params.put("key","something");
        params.put("value","somethingElse");
        ContentType assertion = (ContentType) postWithBodyAndAssertion.getAssertions()[0];
        //simulate failure
        assertion.setContentType("application/xml");
        postWithBodyAndAssertion.run(params);
        assertThat(AssertionStorage.getInstance().getFails("postWithBody returns JSON"), Matchers.is(1));
    }

    @Test
    public void ContentNotEmptyAssertNotFailingIfCorrect() throws InterruptedException {
        val params = new HashMap<String, String>();
        getJsonObjectWithAssertion.run(params);
        assertThat(AssertionStorage.getInstance().getFails("jsonObject returns something"), Matchers.is(0));
    }

    @Test
    public void ContentNotEmptyAssertFailingIfFalse() throws InterruptedException {
        val params = new HashMap<String, String>();
        //simulate failure
        getJsonObjectWithAssertion.setAddr("http://localhost:9000/empty");
        getJsonObjectWithAssertion.run(params);
        assertThat(AssertionStorage.getInstance().getFails("jsonObject returns something"), Matchers.is(1));
    }

    @Test
    public void ResponseAssertNotFailingIfCorrect() throws InterruptedException {
        val params = new HashMap<String, String>();
        params.put("key", HttpHandlers.AuthHandler.username);
        params.put("value", HttpHandlers.AuthHandler.password);
        getWithAuth.run(params);
        assertThat(AssertionStorage.getInstance().getFails("auth does not return 401"), Matchers.is(0));
    }

    @Test
    public void ResponseAssertFailingIfFalse() throws InterruptedException {
        val params = new HashMap<String, String>();
        params.put("key", "wrong");
        params.put("value", "wrong");
        getWithAuth.run(params);
        assertThat(AssertionStorage.getInstance().getFails("auth does not return 401"), Matchers.is(1));
    }
}