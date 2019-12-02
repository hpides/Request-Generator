package de.hpi.tdgt.deserialisation;




import de.hpi.tdgt.requesthandling.HttpConstants;
import de.hpi.tdgt.test.story.atom.Atom;
import de.hpi.tdgt.test.story.atom.assertion.ContentNotEmpty;
import de.hpi.tdgt.test.story.atom.assertion.ContentType;
import de.hpi.tdgt.test.story.atom.assertion.ResponseCode;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import de.hpi.tdgt.test.story.atom.Data_Generation;
import de.hpi.tdgt.test.story.atom.Request;
import de.hpi.tdgt.test.story.atom.Delay;
import de.hpi.tdgt.Utils;

import java.io.IOException;
import java.util.Vector;

import static de.hpi.tdgt.Utils.assertInstanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class DeserializeAtom {
    private String getExampleJSON() throws IOException {
        return new Utils().getExampleJSON();
    }

    private Atom firstAtomOfFirstStory;
    private Atom secondAtomOfFirstStory;
    private Atom sixthAtomOfFirstStory;
    private Atom secondAtomOfSecondStory;
    private Request postWithBodyAndAssertion;
    private Request getJSONObject;
    private Request getWithAuth;

    @BeforeEach
    public void prepareTest() throws IOException {
        firstAtomOfFirstStory = Deserializer.deserialize(getExampleJSON()).getStories()[0].getAtoms()[0];
        secondAtomOfFirstStory = Deserializer.deserialize(getExampleJSON()).getStories()[0].getAtoms()[1];
        sixthAtomOfFirstStory = Deserializer.deserialize(getExampleJSON()).getStories()[0].getAtoms()[5];
        secondAtomOfSecondStory = Deserializer.deserialize(getExampleJSON()).getStories()[1].getAtoms()[3];
        postWithBodyAndAssertion = (Request) Deserializer.deserialize(new Utils().getRequestExampleWithAssertionsJSON()).getStories()[0].getAtoms()[1];
        getJSONObject = (Request) Deserializer.deserialize(new Utils().getRequestExampleWithAssertionsJSON()).getStories()[0].getAtoms()[2];
        getWithAuth = (Request) Deserializer.deserialize(new Utils().getRequestExampleWithAssertionsJSON()).getStories()[0].getAtoms()[3];
    }

    @Test
    public void firstAtomOfFirstStoryIsDataGeneration() {
        assertInstanceOf(firstAtomOfFirstStory, Data_Generation.class);
    }

    @Test
    public void sixthAtomOfFirstStoryIsDelay() {
        assertInstanceOf(sixthAtomOfFirstStory, Delay.class);
    }

    @Test
    public void secondAtomOfSecondStoryIsRequest() {
        assertInstanceOf(secondAtomOfSecondStory, Request.class);
    }

    @Test
    public void firstAtomOfFirstStoryGetsUsernameAndPasswordFromUsers() throws IOException {
        val firstAtomOfFirstStory = (Data_Generation) this.firstAtomOfFirstStory;
        assertArrayEquals(new String[]{"username","password"},firstAtomOfFirstStory.getData());
        assertEquals("users", firstAtomOfFirstStory.getTable());
    }

    @Test
    public void sixthAtomOfFirstStoryWaitsOneSecond() {
        val sixthAtomOfFirstStory = (Delay) this.sixthAtomOfFirstStory;
        assertEquals(1000, sixthAtomOfFirstStory.getDelayMs());
    }

    @Test
    public void secondAtomOfSecondStorySendsGETRequest(){
        val secondAtomOfSecondStory = (Request) this.secondAtomOfSecondStory;
        assertEquals("GET", secondAtomOfSecondStory.getVerb());
    }

    @Test
    public void secondAtomOfSecondStoryHasCorrectAddress(){
        val secondAtomOfSecondStory = (Request) this.secondAtomOfSecondStory;
        assertEquals("http://search/posts/search", secondAtomOfSecondStory.getAddr());
    }

    @Test
    public void secondAtomOfSecondStoryHasCorrectRequestParams(){
        val secondAtomOfSecondStory = (Request) this.secondAtomOfSecondStory;
        assertArrayEquals(new String[]{"key"}, secondAtomOfSecondStory.getRequestParams());
    }

    @Test
    public void secondAtomOfSecondStoryHasCorrectResponseParams(){
        val secondAtomOfSecondStory = (Request) this.secondAtomOfSecondStory;
        assertArrayEquals(null, secondAtomOfSecondStory.getResponseParams());
    }
    @Test
    public void lastAtomOfSecondStoryHasNoSuccessors(){
        assertArrayEquals(new Atom[0], secondAtomOfSecondStory.getSuccessors()[0].getSuccessors());
    }
    @Test
    public void firstAtomOfFirstStoryHasOneSuccessor(){
        assertEquals(1, firstAtomOfFirstStory.getSuccessors().length);
    }
    @Test
    public void firstAtomOfFirstStoryHasCorrectSuccessor(){
        assertEquals(1, firstAtomOfFirstStory.getSuccessors()[0].getId());
    }
    @Test
    public void secondAtomOfFirstStoryHasCorrectSuccessors(){
        val successors = new Vector<Integer>();
        //no implicit parallelism here, we need the exact order for the assert
        for(Atom successor : secondAtomOfFirstStory.getSuccessors()){
            successors.add(successor.getId());
        }
        assertArrayEquals(new Integer[]{2,4}, successors.toArray(new Integer[0]));
    }
    @Test
    public void postWithBodyAndAssertionAssertsContentType(){
        assertThat(postWithBodyAndAssertion.getAssertions()[0], instanceOf(ContentType.class));
    }
    @Test
    public void postWithBodyAndAssertionAssertsContentTypeJSON(){
        ContentType assertion = (ContentType) postWithBodyAndAssertion.getAssertions()[0];
        assertThat(assertion.getContentType(), equalTo(HttpConstants.CONTENT_TYPE_APPLICATION_JSON));
    }
    @Test
    public void getJsonObjectWithAssertionAssertsContentNotEmpty(){
        assertThat(getJSONObject.getAssertions()[0], instanceOf(ContentNotEmpty.class));
    }
    @Test
    public void getWithAuthAndAssertionAssertsResponseCode(){
        assertThat(getWithAuth.getAssertions()[0], instanceOf(ResponseCode.class));
    }
    @Test
    public void getWithAuthAndAssertionAssertsResponseCode200(){
        ResponseCode code = (ResponseCode)  getWithAuth.getAssertions()[0];
        assertThat(code.getResponseCode(), is(200));
    }


}
