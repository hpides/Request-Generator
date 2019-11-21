package de.hpi.tdgt;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.hpi.tdgt.deserialisation.Deserializer;
import de.hpi.tdgt.requesthandling.RestClient;
import de.hpi.tdgt.test.story.activity.Data_Generation;
import lombok.val;
import org.apache.commons.io.IOUtils;
import de.hpi.tdgt.test.Test;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
public class Main {
    public static final String USERNAME="superuser";
    public static final String PASSWORD="somepw";
    public static void main(String[] args) throws IOException {
        if(args.length == 0){
            try {
                System.err.println("Usage: java -jar "+new java.io.File(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getName()+" load <Path to request JSON> <Path to generated Data>");
                System.err.println("or: java -jar "+new java.io.File(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getName()+" testRest");
                System.exit(1);
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
        }
        if(args[0].equals("load") ) {
            try {
                StringWriter writer = new StringWriter();
                IOUtils.copy(new FileInputStream(args[1]), writer);
                String json = writer.toString();
                Test deserializedTest = Deserializer.deserialize(json);
                System.out.println("Successfully deserialized input json including " + deserializedTest.getStories().length + " stories.");
                System.out.println("Running test...");
                Data_Generation.outputDirectory = args[2];
                deserializedTest.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else{
            val rc = new RestClient();
            val params = new HashMap<String, String>();
            params.put("username", USERNAME);
            params.put("password", PASSWORD);

            System.out.println("--- Testing user creation and update ---");
            var result = rc.postBodyToEndpoint(new URL("http://users/users/new"),new ObjectMapper().writeValueAsString(params));
            System.out.println("Create user: "+result.toString()+" and code: "+result.getReturnCode()+" in: "+result.durationMillis()+" ms.");
            result = rc.getFromEndpointWithAuth(new URL("http://users/users/all"),null, USERNAME, PASSWORD);
            System.out.println("Get all users: "+result.toString()+" and code: "+result.getReturnCode()+" in: "+result.durationMillis()+" ms.");
            result = rc.putFormToEndpointWithAuth(new URL("http://users/users/update"),params, USERNAME, PASSWORD);
            System.out.println("Update user: "+result.toString()+" and code: "+result.getReturnCode()+" in: "+result.durationMillis()+" ms.");

            System.out.println("--- Testing post creation ---");
            params.clear();
            params.put("title","A very good post");
            params.put("text", "because it is rather short.");
            result = rc.postFormToEndpointWithAuth(new URL("http://posts/posts/new"),params, USERNAME, PASSWORD);
            System.out.println("Create post: "+result.toString()+" and code: "+result.getReturnCode()+" in: "+result.durationMillis()+" ms.");
            result = rc.getFromEndpointWithAuth(new URL("http://posts/posts/all"),null, USERNAME, PASSWORD);
            System.out.println("Get all posts: "+result.toString()+" and code: "+result.getReturnCode()+" in: "+result.durationMillis()+" ms.");


            System.out.println("--- Testing search ---");
            params.clear();
            params.put("key","short");
            result = rc.getFromEndpointWithAuth(new URL("http://search/posts/search"),params,USERNAME,PASSWORD);
            System.out.println("Search: "+result.toString()+" and code: "+result.getReturnCode()+" in: "+result.durationMillis()+" ms.");

            System.out.println("--- Deleting user ---");
            result = rc.deleteFromEndpointWithAuth(new URL("http://users/users/delete"),null, USERNAME, PASSWORD);
            System.out.println("Delete user: "+result.toString()+" and code: "+result.getReturnCode()+" in: "+result.durationMillis()+" ms.");
        }
    }
}
