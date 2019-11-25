package de.hpi.tdgt.time_measurement;

import com.sun.net.httpserver.HttpServer;
import de.hpi.tdgt.Utils;
import de.hpi.tdgt.deserialisation.Deserializer;
import de.hpi.tdgt.requesthandling.HttpHandlers;
import de.hpi.tdgt.test.story.UserStory;
import de.hpi.tdgt.test.story.activity.Data_Generation;
import de.hpi.tdgt.test.story.activity.Request;
import de.hpi.tdgt.test.time_measurement.TimeStorage;
import lombok.extern.log4j.Log4j2;
import lombok.val;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.greaterThan;
@Log4j2
public class TimeStorageTest {

    private final HttpHandlers.GetHandler getHandler = new HttpHandlers.GetHandler();
    private final HttpHandlers.GetWithBodyHandler getWithBodyHandler = new HttpHandlers.GetWithBodyHandler();
    private final HttpHandlers.JSONObjectGetHandler jsonObjectGetHandler = new HttpHandlers.JSONObjectGetHandler();
    private final HttpHandlers.JSONArrayGetHandler jsonArrayGetHandler = new HttpHandlers.JSONArrayGetHandler();
    private final HttpHandlers.PostHandler postHandler = new HttpHandlers.PostHandler();
    private final HttpHandlers.PostBodyHandler postBodyHandler = new HttpHandlers.PostBodyHandler();
    private final HttpHandlers.PostBodyHandler putBodyHandler = new HttpHandlers.PostBodyHandler();
    private final HttpHandlers.AuthHandler authHandler = new HttpHandlers.AuthHandler();
    private HttpServer server;

    //Based on https://www.codeproject.com/tips/1040097/create-a-simple-web-server-in-java-http-server
    @BeforeEach
    public void launchTestServer() throws IOException {
        int port = 9000;
        server = HttpServer.create(new InetSocketAddress(port), 0);
        log.info("server started at " + port);
        server.createContext("/", getHandler);
        server.createContext("/getWithBody", getWithBodyHandler);
        server.createContext("/jsonObject", jsonObjectGetHandler);
        server.createContext("/jsonArray", jsonArrayGetHandler);
        server.createContext("/echoPost", postHandler);
        server.createContext("/postWithBody", postBodyHandler);
        server.createContext("/putWithBody", putBodyHandler);
        server.createContext("/auth", authHandler);
        server.setExecutor(null);
        server.start();

        File values = new File("values.csv");
        values.deleteOnExit();
        var os = new FileOutputStream(values);
        IOUtils.copy(new Utils().getValuesCSV(), os);
        os.close();
    }

    @AfterEach
    public void removeSideEffects(){
        //clean side effects
        authHandler.setNumberFailedLogins(0);
        authHandler.setTotalRequests(0);
        jsonObjectGetHandler.setRequestsTotal(0);
        Data_Generation.reset();
        server.stop(0);
    }
    @Test
    public void testFirstRequestOfFirstStoryTakesTime() throws IOException, InterruptedException {
        de.hpi.tdgt.test.Test test = Deserializer.deserialize(new Utils().getRequestExampleJSON());
        //do not run second story for this time around; messes with results
        test.setStories(new UserStory[]{test.getStories()[0]});
        test.start();
        val storage = TimeStorage.getInstance();
        Request firstRequest = (Request) test.getStories()[0].getActivities()[1];
        assertThat(storage.getTimes(firstRequest.getVerb(), firstRequest.getAddr()).length, greaterThan(0));
    }

    @Test
    public void testFirstRequestOfFirstStoryHasMaxTimeOverNull() throws IOException, InterruptedException {
        de.hpi.tdgt.test.Test test = Deserializer.deserialize(new Utils().getRequestExampleJSON());
        //do not run second story for this time around; messes with results
        test.setStories(new UserStory[]{test.getStories()[0]});
        test.start();
        val storage = TimeStorage.getInstance();
        Request firstRequest = (Request) test.getStories()[0].getActivities()[1];
        assertThat(storage.getMax(firstRequest.getVerb(), firstRequest.getAddr()), greaterThan(0L));
    }
    @Test
    public void testFirstRequestOfFirstStoryHasMinTimeOverNull() throws IOException, InterruptedException {
        de.hpi.tdgt.test.Test test = Deserializer.deserialize(new Utils().getRequestExampleJSON());
        //do not run second story for this time around; messes with results
        test.setStories(new UserStory[]{test.getStories()[0]});
        test.start();
        val storage = TimeStorage.getInstance();
        Request firstRequest = (Request) test.getStories()[0].getActivities()[1];
        assertThat(storage.getMin(firstRequest.getVerb(), firstRequest.getAddr()), greaterThan(0L));
    }
    @Test
    public void testFirstRequestOfFirstStoryHasAvgTimeOverNull() throws IOException, InterruptedException {
        de.hpi.tdgt.test.Test test = Deserializer.deserialize(new Utils().getRequestExampleJSON());
        //do not run second story for this time around; messes with results
        test.setStories(new UserStory[]{test.getStories()[0]});
        test.start();
        val storage = TimeStorage.getInstance();
        Request firstRequest = (Request) test.getStories()[0].getActivities()[1];
        assertThat(storage.getAvg(firstRequest.getVerb(), firstRequest.getAddr()), greaterThan(0d));
    }
}
