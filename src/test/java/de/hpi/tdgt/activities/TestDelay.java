package de.hpi.tdgt.activities;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.hpi.tdgt.test.story.activity.Delay;

import lombok.val;

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
        assertTrue((endTime - startTime) > 100);
    }

    @Test
    public void delay1000ms() {
        delayActivity.setDelayMs(1000);
        val startTime = System.currentTimeMillis();
        delayActivity.perform();
        val endTime = System.currentTimeMillis();
        assertTrue((endTime - startTime) > 1000);
    }
}