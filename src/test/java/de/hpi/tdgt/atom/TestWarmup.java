package de.hpi.tdgt.atom;

import de.hpi.tdgt.HttpHandlers;
import de.hpi.tdgt.RequestHandlingFramework;
import de.hpi.tdgt.Utils;
import de.hpi.tdgt.deserialisation.Deserializer;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

public class TestWarmup extends RequestHandlingFramework {

    private de.hpi.tdgt.test.Test warmupTest;

    @BeforeEach
    public void prepare() throws IOException {
        warmupTest = Deserializer.deserialize(new Utils().getRequestExampleWithAssertionsAndWarmupJSON());
    }
    @Test
    public void testWarmupCallsPreparationActivities() throws InterruptedException, ExecutionException {
        val threads = warmupTest.warmup();
        assertThat(postBodyHandler.getRequests_total(), is(7));
        warmupTest.start(threads);
    }

    @Test
    public void testWarmupCallsNoOtherActivities() throws InterruptedException, ExecutionException {
        val threads = warmupTest.warmup();
        for(val handler : handlers) {
            if(!(handler instanceof HttpHandlers.PostBodyHandler)) {
                assertThat(handler.getRequests_total(), is(0));
            }
        }
        warmupTest.start(threads);
    }
    @Test
    public void testStoriesAreCompletedAfterWarmup() throws InterruptedException, ExecutionException {

        val threads = warmupTest.warmup();
        warmupTest.start(threads);
        //7 in first story, 30 in second story
        assertThat(authHandler.getRequests_total(), is(37));
    }

    @Test
    public void testStoriesAreCompletedAfterWarmupWithRepeat() throws InterruptedException, ExecutionException {
        warmupTest.setRepeat(3);
        val threads = warmupTest.warmup();
        warmupTest.start(threads);
        //7 in first story, 30 in second story
        assertThat(authHandler.getRequests_total(), is(3*37));
    }
    @Test
    public void testFirstStoryHasWarmup(){
        assertThat(warmupTest.getStories()[0].hasWarmup(), is(true));
    }

    @Test
    public void testSecondStoryHasNoWarmup(){
        assertThat(warmupTest.getStories()[1].hasWarmup(), is(false));
    }
    @Test
    public void testSecondStoryHasNoWarmupInNumbers(){
        assertThat(warmupTest.getStories()[1].numberOfWarmupEnds(), is(0));
    }
    @Test
    public void testFirstStoryHasOneWarmupInNumbers(){
        assertThat(warmupTest.getStories()[0].numberOfWarmupEnds(), is(1));
    }
}
