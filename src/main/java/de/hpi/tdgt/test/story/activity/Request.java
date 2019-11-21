package de.hpi.tdgt.test.story.activity;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.hpi.tdgt.requesthandling.RestClient;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
public class Request extends Activity {
    private String verb;
    private String addr;
    /**
     * Expected usage: values of this arrays are keys. Use them as keys in a HTTP
     * Form in a Request Body, get values for these keys from passed dict.
     */
    private String[] requestParams;
    /**
     * Expected usage: values of this arrays are keys. Get the values for these keys
     * from a response body Form and store them in the dict passed to the
     * successors.
     */
    private String[] responseParams;
    /**
     * Expected usage: values of this arrays are keys. Use them as keys in a JSON
     * Object in a Request Body, get values for these keys from passed dict.
     */
    private String[] requestJSONObject;
    /**
     * Expected usage: values of this arrays are keys. Get the values for these keys
     * from a response body JSON object and store them in the dict passed to the
     * successors.
     */
    private String[] responseJSONObject;

    private BasicAuth basicAuth;
    private static RestClient rc = new RestClient();
    private static ObjectMapper om = new ObjectMapper();

    @Override
    public void perform() {
        System.out.println("Sending request "+addr);
        switch (verb) {
        case "POST":
            handlePost();
            break;
        case "PUT":
            handlePut();
            break;
        case "DELETE":
            handleDelete();
            break;
        case "GET":
            handleGet();
            break;
        }
    }

    private void handlePost() {
        if (requestJSONObject != null) {
            handlePostWithBody();
        } else {
            handlePostWithForm();
        }
    }

    private void handlePostWithForm() {
        val params = new HashMap<String, String>();
        if (requestParams != null) {
            for (val key : requestParams) {
                params.put(key, getKnownParams().get(key));
            }
        }
        
        if (basicAuth == null) {
            try {
                extractResponseParams(rc.postFormToEndpoint(new URL(this.addr), params));
            } catch (MalformedURLException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        } else {
            try {
                extractResponseParams(
                    rc.postFormToEndpointWithAuth(new URL(this.addr), params, getKnownParams().get(basicAuth.getUser()),
                        getKnownParams().get(basicAuth.getPassword()))
                );
            } catch (MalformedURLException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }
    private Map<String, String> toStringMap(Map input){
        val ret = new HashMap<String, String>();
        for(Object key : input.keySet()){
            ret.put(key.toString(), input.get(key).toString());
        }
        return ret;

    }
    private void extractResponseParams(final de.hpi.tdgt.requesthandling.RestResult result)
            throws IOException, JsonParseException, JsonMappingException {
        String json = new String(result.getResponse(), StandardCharsets.UTF_8);
        val map = om.readValue(json, Map.class);
        getKnownParams().putAll(toStringMap(map));
    }

    private void handlePostWithBody() {
        val params = new HashMap<String, String>();
        if (requestJSONObject != null) {
            for (val key : requestJSONObject) {
                params.put(key, getKnownParams().get(key));
            }
        }
        var jsonParams = "";
        try {
            jsonParams = om.writeValueAsString(params);
        } catch (JsonProcessingException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        
        if (basicAuth == null) {
            try {
                extractResponseParams(rc.postBodyToEndpoint(new URL(this.addr), jsonParams));
            } catch (MalformedURLException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        } else {
            try {
                extractResponseParams(
                    rc.postBodyToEndpointWithAuth(new URL(this.addr), jsonParams, getKnownParams().get(basicAuth.getUser()),
                        getKnownParams().get(basicAuth.getPassword()))
                );
            } catch (MalformedURLException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    private void handlePut() {
        // TODO implement
    }

    private void handleDelete() {
        val params = new HashMap<String, String>();
        if (requestParams != null) {
            for (val key : requestParams) {
                params.put(key, getKnownParams().get(key));
            }
        }
        
        if (basicAuth == null) {
            try {
                rc.deleteFromEndpoint(new URL(this.addr), params);
            } catch (MalformedURLException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        } else {
            try {
                rc.deleteFromEndpointWithAuth(new URL(this.addr), params, getKnownParams().get(basicAuth.getUser()),
                    getKnownParams().get(basicAuth.getPassword()));
            } catch (MalformedURLException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    private void handleGet() {
        val params = new HashMap<String, String>();
        if (requestParams != null) {
            for (val key : requestParams) {
                params.put(key, getKnownParams().get(key));
            }
        }
        
        if (basicAuth == null) {
            try {
                rc.getFromEndpoint(new URL(this.addr), params);
            } catch (MalformedURLException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        } else {
            try {
                rc.getFromEndpointWithAuth(new URL(this.addr), params, getKnownParams().get(basicAuth.getUser()),
                    getKnownParams().get(basicAuth.getPassword()));
            } catch (MalformedURLException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    @Getter
    @Setter
    @NoArgsConstructor
    private class BasicAuth{
        private String user;
        private String password;
    }
}
