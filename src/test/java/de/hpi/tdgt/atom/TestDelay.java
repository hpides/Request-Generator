package de.hpi.tdgt.atom;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.hpi.tdgt.test.story.atom.Delay;

import lombok.val;

public class TestDelay {

    private Delay delayAtom;

    @BeforeEach
    public void prepareTest() {
        delayAtom = new Delay();
    }

    @Test
    public void delay100ms() {
        delayAtom.setDelayMs(100);
        val startTime = System.currentTimeMillis();
        delayAtom.perform();
        val endTime = System.currentTimeMillis();
        assertTrue((endTime - startTime) > 90);
    }

    @Test
    public void delay1000ms() {
        delayAtom.setDelayMs(1000);
        val startTime = System.currentTimeMillis();
        delayAtom.perform();
        val endTime = System.currentTimeMillis();
        assertTrue((endTime - startTime) > 990);
    }

    @Test
    public void cloneCreatesEquivalentObject() {
        val clone = delayAtom.clone();
        assertThat(clone, equalTo(delayAtom));
    }

    @Test
    public void cloneCreatesOtherObject() {
        val clone = delayAtom.clone();
        assertNotSame(clone, delayAtom);
    }
}