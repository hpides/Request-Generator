package de.hpi.tdgt.atom;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertNotSame;

import de.hpi.tdgt.HttpHandlers;
import de.hpi.tdgt.RequestHandlingFramework;
import de.hpi.tdgt.Utils;
import de.hpi.tdgt.deserialisation.Deserializer;
import de.hpi.tdgt.test.story.atom.Request;
import de.hpi.tdgt.test.story.atom.assertion.Assertion;
import de.hpi.tdgt.test.story.atom.assertion.AssertionStorage;
import de.hpi.tdgt.test.story.atom.assertion.ContentType;
import de.hpi.tdgt.test.story.atom.assertion.ResponseCode;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import lombok.val;

import java.io.IOException;
import java.util.HashMap;

public class TestRequest extends RequestHandlingFramework {

    private Request requestAtom;
    private Request postWithBodyAndAssertion;
    private Request getJsonObjectWithAssertion;
    private Request getWithAuth;
    @BeforeEach
    public void prepareTest() throws IOException {
        requestAtom = new Request();
        requestAtom.setVerb("GET");
        requestAtom.setAddr("http://example.com");
        requestAtom.setAssertions(new Assertion[]{new ResponseCode()});
        postWithBodyAndAssertion = (Request) Deserializer.deserialize(new Utils().getRequestExampleWithAssertionsJSON()).getStories()[0].getAtoms()[1];
        getJsonObjectWithAssertion = (Request) Deserializer.deserialize(new Utils().getRequestExampleWithAssertionsJSON()).getStories()[0].getAtoms()[2];
        getWithAuth = (Request) Deserializer.deserialize(new Utils().getRequestExampleWithAssertionsJSON()).getStories()[0].getAtoms()[3];
    }
    @AfterEach
    public void clearAssertions(){
        AssertionStorage.getInstance().reset();
    }

    @Test
    public void cloneCreatesEquivalentObject() {
        val clone = requestAtom.clone();
        assertThat(clone, equalTo(requestAtom));
    }

    @Test
    public void cloneCreatesEquivalentObjectWhenAllAttribvutesAreSet() {
        requestAtom.setResponseJSONObject(new String[]{"item1", "item2"});
        //noch 10
        requestAtom.setResponseParams(new String[]{"item3", "item4"});
        requestAtom.setRequestJSONObject("{\"item5\":$item5, \"item6\":$item6}");
        requestAtom.setRequestParams(new String[]{"item7", "item8"});
        requestAtom.setBasicAuth(new Request.BasicAuth("user","pw"));
        requestAtom.setId(0);
        requestAtom.setName("Some Request");
        requestAtom.setPredecessorCount(1);
        requestAtom.setRepeat(3);
        requestAtom.setSuccessors(new int[0]);

        val clone = requestAtom.clone();
        //would have been set by story
        clone.setPredecessorCount(1);
        assertThat(clone, equalTo(requestAtom));
    }

    @Test
    public void cloneCreatesotherObject() {
        val clone = requestAtom.clone();
        assertNotSame(clone, requestAtom);
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
    public void ContentTypeAssertHasCorrectContentType() throws InterruptedException {
        val params = new HashMap<String, String>();
        params.put("key","something");
        params.put("value","somethingElse");
        ContentType assertion = (ContentType) postWithBodyAndAssertion.getAssertions()[0];
        //simulate failure
        assertion.setContentType("application/xml");
        postWithBodyAndAssertion.run(params);
        assertThat(AssertionStorage.getInstance().getActual("postWithBody returns JSON"), contains("application/json"));
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
    public void ContentNotEmptyAssertHasCorrectContent() throws InterruptedException {
        val params = new HashMap<String, String>();
        //simulate failure
        getJsonObjectWithAssertion.setAddr("http://localhost:9000/empty");
        getJsonObjectWithAssertion.run(params);
        assertThat(AssertionStorage.getInstance().getActual("jsonObject returns something"), contains(""));
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

    @Test
    public void ResponseAssertHasCorrectResponseCode() throws InterruptedException {
        val params = new HashMap<String, String>();
        params.put("key", "wrong");
        params.put("value", "wrong");
        getWithAuth.run(params);
        assertThat(AssertionStorage.getInstance().getActual("auth does not return 401"), contains("401"));
    }
    @Test
    public void ResponseAssertHasCorrectResponseCodeForDelete() throws InterruptedException {
        val params = new HashMap<String, String>();
        params.put("key", "wrong");
        params.put("value", "wrong");
        getWithAuth.setVerb("DELETE");
        getWithAuth.run(params);
        assertThat(AssertionStorage.getInstance().getActual("auth does not return 401"), contains("401"));
    }

    @Test
    public void resetOfAssertWorks() throws InterruptedException {
        val params = new HashMap<String, String>();
        params.put("key", "wrong");
        params.put("value", "wrong");
        getWithAuth.run(params);
        AssertionStorage.getInstance().reset();
        assertThat(AssertionStorage.getInstance().getActual("auth does not return 401"), empty());
    }


}