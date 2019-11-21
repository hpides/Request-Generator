package de.hpi.tdgt.activities;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.hpi.tdgt.test.story.activity.Request;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.hpi.tdgt.test.story.activity.Delay;

import lombok.val;

import java.util.HashMap;
import java.util.Map;

public class TestRequest {

    private Request requestActivity;

    @BeforeEach
    public void prepareTest() {
        requestActivity = new Request();
        requestActivity.setVerb("GET");
        requestActivity.setAddr("http://example.com");
    }

    @Test
    public void cloneCreatesEquivalentObject() {
        Map<String, String> params = new HashMap<>();
        val clone = requestActivity.clone();
        assertThat(clone, equalTo(requestActivity));
    }

    @Test
    public void cloneCreatesEquivalentObjectWhenAllAttribvutesAreSet() {
        Map<String, String> params = new HashMap<>();
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
        assertThat(clone, equalTo(requestActivity));
    }

    @Test
    public void cloneCreatesotherObject() {
        Map<String, String> params = new HashMap<>();
        val clone = requestActivity.clone();
        assertNotSame(clone, requestActivity);
    }
}