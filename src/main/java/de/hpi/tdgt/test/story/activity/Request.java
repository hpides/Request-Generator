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
@EqualsAndHashCode(callSuper = true)
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

    @Override
    public Activity performClone() {
        val ret = new Request();
        ret.setAddr(this.getAddr());
        ret.setVerb(this.getVerb());
        ret.setResponseJSONObject(this.getResponseJSONObject());
        ret.setRequestParams(this.getRequestParams());
        ret.setBasicAuth(this.getBasicAuth());
        ret.setRequestJSONObject(this.getRequestJSONObject());
        ret.setResponseJSONObject(this.getResponseJSONObject());
        ret.setResponseParams(this.getResponseParams());
        return ret;
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
        if(result.isJSON()) {
            if(result.toJson().isObject()) {
                String json = new String(result.getResponse(), StandardCharsets.UTF_8);
                val map = om.readValue(json, Map.class);
                getKnownParams().putAll(toStringMap(map));
            }
            else{
                System.err.println("I can not handle Arrays.");
                System.err.println(result);
            }
        }
        else {
            System.err.println("Not JSON! Response is ignored.");
            System.err.println(result);
        }
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
        if (requestJSONObject != null) {
            handlePutWithBody();
        } else {
            handlePutWithForm();
        }
    }

    private void handlePutWithForm() {
        val params = new HashMap<String, String>();
        if (requestParams != null) {
            for (val key : requestParams) {
                params.put(key, getKnownParams().get(key));
            }
        }
        
        if (basicAuth == null) {
            try {
                extractResponseParams(rc.putFormToEndpoint(new URL(this.addr), params));
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
                    rc.putFormToEndpointWithAuth(new URL(this.addr), params, getKnownParams().get(basicAuth.getUser()),
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

    private void handlePutWithBody() {
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
                extractResponseParams(rc.putBodyToEndpoint(new URL(this.addr), jsonParams));
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
                    rc.putBodyToEndpointWithAuth(new URL(this.addr), jsonParams, getKnownParams().get(basicAuth.getUser()),
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
        if (requestJSONObject != null) {
            handleGetWithBody();
        } else {
            handleGetWithForm();
        }
    }

    private void handleGetWithForm() {
        val params = new HashMap<String, String>();
        if (requestParams != null) {
            for (val key : requestParams) {
                params.put(key, getKnownParams().get(key));
            }
        }
        
        if (basicAuth == null) {
            try {
                extractResponseParams(rc.getFromEndpoint(new URL(this.addr), params));
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
                    rc.getFromEndpointWithAuth(new URL(this.addr), params, getKnownParams().get(basicAuth.getUser()),
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

    private void handleGetWithBody() {
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
                extractResponseParams(rc.getBodyFromEndpoint(new URL(this.addr), jsonParams));
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
                    rc.getBodyFromEndpointWithAuth(new URL(this.addr), jsonParams, getKnownParams().get(basicAuth.getUser()),
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

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BasicAuth{
        private String user;
        private String password;
    }
}
