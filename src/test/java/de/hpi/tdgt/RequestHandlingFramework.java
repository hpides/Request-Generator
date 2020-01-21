package de.hpi.tdgt;

import com.sun.net.httpserver.HttpServer;
import de.hpi.tdgt.test.ThreadRecycler;
import de.hpi.tdgt.test.story.atom.Data_Generation;
import de.hpi.tdgt.test.story.atom.WarmupEnd;
import de.hpi.tdgt.test.story.atom.assertion.AssertionStorage;
import de.hpi.tdgt.test.time_measurement.TimeStorage;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

@Log4j2
public class RequestHandlingFramework {
    protected final HttpHandlers.GetHandler getHandler = new HttpHandlers.GetHandler();
    protected final HttpHandlers.GetWithBodyHandler getWithBodyHandler = new HttpHandlers.GetWithBodyHandler();
    protected final HttpHandlers.JSONObjectGetHandler jsonObjectGetHandler = new HttpHandlers.JSONObjectGetHandler();
    protected final HttpHandlers.JSONArrayGetHandler jsonArrayGetHandler = new HttpHandlers.JSONArrayGetHandler();
    protected final HttpHandlers.PostHandler postHandler = new HttpHandlers.PostHandler();
    protected final HttpHandlers.PostBodyHandler postBodyHandler = new HttpHandlers.PostBodyHandler();
    protected final HttpHandlers.PostBodyHandler putBodyHandler = new HttpHandlers.PostBodyHandler();
    protected final HttpHandlers.AuthHandler authHandler = new HttpHandlers.AuthHandler();
    protected final HttpHandlers.EmptyResponseHandler emptyResponseHandler = new HttpHandlers.EmptyResponseHandler();
    protected HttpServer server;
    protected List<HttpHandlers.HttpHandlerBase> handlers = new ArrayList<>();
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
        server.createContext("/empty", emptyResponseHandler);
        server.setExecutor(null);
        server.start();
        handlers.add(getHandler);
        handlers.add(getWithBodyHandler);
        handlers.add(jsonObjectGetHandler);
        handlers.add(jsonArrayGetHandler);
        handlers.add(postHandler);
        handlers.add(postBodyHandler);
        handlers.add(putBodyHandler);
        handlers.add(authHandler);
        handlers.add(emptyResponseHandler);
        File values = new File("values.csv");
        values.deleteOnExit();
        var os = new FileOutputStream(values);
        IOUtils.copy(new Utils().getValuesCSV(), os);
        os.close();
        //tests want predictable behaviour in regards to when an entry is stored
        AssertionStorage.getInstance().setStoreEntriesAsynch(false);
    }

    @AfterEach
    public void removeSideEffects(){
        //clean side effects
        authHandler.setNumberFailedLogins(0);
        authHandler.setTotalRequests(0);
        jsonObjectGetHandler.setRequestsTotal(0);
        Data_Generation.reset();
        handlers.forEach(handler -> handler.setRequests_total(0));
        handlers = new ArrayList<>();
        server.stop(0);
        TimeStorage.getInstance().reset();
        AssertionStorage.getInstance().reset();
        ThreadRecycler.getInstance().getExecutorService().shutdown();
    }
}
