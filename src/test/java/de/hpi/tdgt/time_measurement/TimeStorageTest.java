package de.hpi.tdgt.time_measurement;

import de.hpi.tdgt.RequestHandlingFramework;
import de.hpi.tdgt.Utils;
import de.hpi.tdgt.deserialisation.Deserializer;
import de.hpi.tdgt.test.story.UserStory;
import de.hpi.tdgt.test.story.atom.Request;
import de.hpi.tdgt.test.time_measurement.TimeStorage;
import lombok.extern.log4j.Log4j2;
import lombok.val;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
@Log4j2
public class TimeStorageTest extends RequestHandlingFramework {


    @Test
    public void testFirstRequestOfFirstStoryTakesTime() throws IOException, InterruptedException, ExecutionException {
        de.hpi.tdgt.test.Test test = Deserializer.deserialize(new Utils().getRequestExampleJSON());
        //do not run second story for this time around; messes with results
        test.setStories(new UserStory[]{test.getStories()[0]});
        test.start();
        val storage = TimeStorage.getInstance();
        Request firstRequest = (Request) test.getStories()[0].getAtoms()[2];
        assertThat(storage.getTimes(firstRequest.getVerb(), firstRequest.getAddr()).length, greaterThan(0));
    }

    @Test
    public void testFirstRequestOfFirstStoryHasMaxTimeOverNull() throws IOException, InterruptedException, ExecutionException {
        de.hpi.tdgt.test.Test test = Deserializer.deserialize(new Utils().getRequestExampleJSON());
        //do not run second story for this time around; messes with results
        test.setStories(new UserStory[]{test.getStories()[0]});
        test.start();
        val storage = TimeStorage.getInstance();
        Request firstRequest = (Request) test.getStories()[0].getAtoms()[2];
        assertThat(storage.getMax(firstRequest.getVerb(), firstRequest.getAddr()), greaterThan(0L));
    }
    @Test
    public void testFirstRequestOfFirstStoryHasMinTimeOverNull() throws IOException, InterruptedException, ExecutionException {
        de.hpi.tdgt.test.Test test = Deserializer.deserialize(new Utils().getRequestExampleJSON());
        //do not run second story for this time around; messes with results
        test.setStories(new UserStory[]{test.getStories()[0]});
        test.start();
        val storage = TimeStorage.getInstance();
        Request firstRequest = (Request) test.getStories()[0].getAtoms()[2];
        assertThat(storage.getMin(firstRequest.getVerb(), firstRequest.getAddr()), greaterThan(0L));
    }
    @Test
    public void testFirstRequestOfFirstStoryHasAvgTimeOverNull() throws IOException, InterruptedException, ExecutionException {
        de.hpi.tdgt.test.Test test = Deserializer.deserialize(new Utils().getRequestExampleJSON());
        //do not run second story for this time around; messes with results
        test.setStories(new UserStory[]{test.getStories()[0]});
        test.start();
        val storage = TimeStorage.getInstance();
        Request firstRequest = (Request) test.getStories()[0].getAtoms()[2];
        //runs asynch
        Thread.sleep(1000);
        assertThat(storage.getAvg(firstRequest.getVerb(), firstRequest.getAddr()), greaterThan(0d));
    }
}
