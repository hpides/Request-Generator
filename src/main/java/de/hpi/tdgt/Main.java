package de.hpi.tdgt;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.hpi.tdgt.deserialisation.Deserializer;
import de.hpi.tdgt.requesthandling.RestClient;
import de.hpi.tdgt.test.Test;
import de.hpi.tdgt.controllers.UploadController;
import de.hpi.tdgt.test.story.atom.Data_Generation;
import de.hpi.tdgt.test.story.atom.assertion.AssertionStorage;
import de.hpi.tdgt.test.time_measurement.TimeStorage;
import lombok.extern.log4j.Log4j2;
import lombok.val;
import org.apache.commons.io.IOUtils;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;

@Log4j2
public class Main {
    public static final String USERNAME="superuser";
    public static final String PASSWORD="somepw";
    public static void main(String[] args) throws IOException, InterruptedException {
        if(args.length == 1){
            try {
                log.error("Usage: java -jar "+new java.io.File(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getName()+" cli load <Path to request JSON> <Path to generated Data>");
                log.error("or: java -jar "+new java.io.File(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getName()+" cli testRest");
                System.exit(1);
            } catch (URISyntaxException e) {
                log.error(e);
            }
        }
        if(args[1].equals("load") ) {
            try {
                StringWriter writer = new StringWriter();
                IOUtils.copy(new FileInputStream(args[2]), writer);
                String json = writer.toString();
                Test deserializedTest = Deserializer.deserialize(json);
                log.info("Successfully deserialized input json including " + deserializedTest.getStories().length + " stories.");
                log.info("Running test...");
                Data_Generation.outputDirectory = args[3];
                //in case warmup is added
                new UploadController().uploadTestConfig(deserializedTest);
            } catch (IOException | ExecutionException e) {
                log.error(e);
            }
        } else{
            val rc = new RestClient();
            val params = new HashMap<String, String>();
            params.put("username", USERNAME);
            params.put("password", PASSWORD);

            log.info("--- Testing user creation and update ---");
            var result = rc.postBodyToEndpoint("REST Test", new URL("http://users/users/new"),new ObjectMapper().writeValueAsString(params));
            log.info("Create user: "+result.toString()+" and code: "+result.getReturnCode()+" in: "+result.durationMillis()+" ms.");
            result = rc.getFromEndpointWithAuth("REST Test", new URL("http://users/users/all"),null, USERNAME, PASSWORD);
            log.info("Get all users: "+result.toString()+" and code: "+result.getReturnCode()+" in: "+result.durationMillis()+" ms.");
            result = rc.putFormToEndpointWithAuth("REST Test", new URL("http://users/users/update"),params, USERNAME, PASSWORD);
            log.info("Update user: "+result.toString()+" and code: "+result.getReturnCode()+" in: "+result.durationMillis()+" ms.");

            log.info("--- Testing post creation ---");
            params.clear();
            params.put("title","A very good post");
            params.put("text", "because it is rather short.");
            result = rc.postFormToEndpointWithAuth("REST Test", new URL("http://posts/posts/new"),params, USERNAME, PASSWORD);
            log.info("Create post: "+result.toString()+" and code: "+result.getReturnCode()+" in: "+result.durationMillis()+" ms.");
            result = rc.getFromEndpointWithAuth("REST Test", new URL("http://posts/posts/all"),null, USERNAME, PASSWORD);
            log.info("Get all posts: "+result.toString()+" and code: "+result.getReturnCode()+" in: "+result.durationMillis()+" ms.");


            log.info("--- Testing search ---");
            params.clear();
            params.put("key","short");
            result = rc.getFromEndpointWithAuth("REST Test", new URL("http://search/posts/search"),params,USERNAME,PASSWORD);
            log.info("Search: "+result.toString()+" and code: "+result.getReturnCode()+" in: "+result.durationMillis()+" ms.");

            log.info("--- Deleting user ---");
            result = rc.deleteFromEndpointWithAuth("REST Test", new URL("http://users/users/delete"),null, USERNAME, PASSWORD);
            log.info("Delete user: "+result.toString()+" and code: "+result.getReturnCode()+" in: "+result.durationMillis()+" ms.");
        }
    }
}
