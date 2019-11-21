package de.hpi.tdgt.activities;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.hpi.tdgt.test.story.activity.Delay;

import lombok.val;

import java.util.HashMap;
import java.util.Map;

public class TestDelay {

    private Delay delayActivity;

    @BeforeEach
    public void prepareTest() {
        delayActivity = new Delay();
    }

    @Test
    public void delay100ms() {
        delayActivity.setDelayMs(100);
        val startTime = System.currentTimeMillis();
        delayActivity.perform();
        val endTime = System.currentTimeMillis();
        assertTrue((endTime - startTime) > 90);
    }

    @Test
    public void delay1000ms() {
        delayActivity.setDelayMs(1000);
        val startTime = System.currentTimeMillis();
        delayActivity.perform();
        val endTime = System.currentTimeMillis();
        assertTrue((endTime - startTime) > 990);
    }

    @Test
    public void cloneCreatesEquivalentObject() {
        Map<String, String> params = new HashMap<>();
        val clone = delayActivity.clone();
        assertThat(clone, equalTo(delayActivity));
    }

    @Test
    public void cloneCreatesOtherObject() {
        Map<String, String> params = new HashMap<>();
        val clone = delayActivity.clone();
        assertNotSame(clone, delayActivity);
    }
}