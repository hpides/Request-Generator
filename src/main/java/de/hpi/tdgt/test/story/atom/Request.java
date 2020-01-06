package de.hpi.tdgt.test.story.atom;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.hpi.tdgt.requesthandling.RestClient;
import de.hpi.tdgt.requesthandling.RestResult;
import de.hpi.tdgt.test.story.atom.assertion.Assertion;
import lombok.*;
import lombok.extern.log4j.Log4j2;

@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Log4j2
public class Request extends Atom {
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
    private String requestJSONObject;
    /**
     * Expected usage: values of this arrays are keys. Get the values for these keys
     * from a response body JSON object and store them in the dict passed to the
     * successors.
     */
    private String[] responseJSONObject;

    private BasicAuth basicAuth;
    @JsonIgnore
    private static RestClient rc = new RestClient();
    @JsonIgnore
    private static ObjectMapper om = new ObjectMapper();

    private Assertion[] assertions = new Assertion[0];

    @Override
    public void perform() {
        log.info("Sending request " + addr + " in Thread " + Thread.currentThread().getId() + "with attributes: " + getKnownParams());
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
    public Atom performClone() {
        val ret = new Request();
        ret.setAddr(this.getAddr());
        ret.setVerb(this.getVerb());
        ret.setResponseJSONObject(this.getResponseJSONObject());
        ret.setRequestParams(this.getRequestParams());
        ret.setBasicAuth(this.getBasicAuth());
        ret.setRequestJSONObject(this.getRequestJSONObject());
        ret.setResponseJSONObject(this.getResponseJSONObject());
        ret.setResponseParams(this.getResponseParams());
        //also stateless
        ret.setAssertions(this.assertions);
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
                rc.postFormToEndpoint(this.getStoryName(), this, new URL(this.addr), params);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                log.error(e);
            }
        } else {
            try {

                rc.postFormToEndpointWithAuth(this.getStoryName(), this, new URL(this.addr), params, getKnownParams().get(basicAuth.getUser()),
                        getKnownParams().get(basicAuth.getPassword())
                );
            } catch (IOException e) {
                // TODO Auto-generated catch block
                log.error(e);
            }
        }
    }

    private Map<String, String> toStringMap(Map input) {
        val ret = new HashMap<String, String>();
        for (Object key : input.keySet()) {
            ret.put(key.toString(), input.get(key).toString());
        }
        return ret;

    }

    /**
     * Will be called as soon as a request result is available.
     *
     * @param result contains the information from the request
     * @throws IOException          can not read loaded bytes
     * @throws JsonParseException   response is not valid json, but is indicated as such by content type
     * @throws JsonMappingException could not map the json to map
     */
    public void extractResponseParams(final RestResult result)
            throws IOException, JsonParseException, JsonMappingException {
        if (result != null && result.isJSON()) {
            if (result.toJson().isObject()) {
                String json = new String(result.getResponse(), StandardCharsets.UTF_8);
                val map = om.readValue(json, Map.class);
                getKnownParams().putAll(toStringMap(map));
            } else {
                log.info("I can not handle Arrays.");
                log.info(result);
            }
        } else {
            log.warn("Not JSON! Response is ignored.");
            log.warn(result);
        }
        //check assertions after request
        for (val assertion : assertions) {
            assertion.check(result);
        }
        try {
            runSuccessors();
        } catch (InterruptedException | ExecutionException e) {
            log.error(e);
        }
    }

    private String fillEvaluationsInJson() {
        String current = requestJSONObject;
        for (val entry : getKnownParams().entrySet()) {
            current = current.replaceAll("\\$" + entry.getKey(), '\"' + entry.getValue() + '\"');
        }
        //should show a warning
        if (Pattern.matches("\\$[a-zA-Z]*", current)) {
            Pattern p = Pattern.compile("\\$[a-zA-Z]*");
            Matcher m = p.matcher(current);
            val allUncompiled = new HashSet<String>();
            while (m.find()) {
                allUncompiled.add(m.group());
            }
            StringBuilder builder = new StringBuilder();
            boolean first = true;
            for (String unmatched : allUncompiled) {
                if (!first) {
                    builder.append(',');
                }
                first = false;
                builder.append(' ').append(unmatched);
            }
            log.warn("Request " + getName() + ": Could not replace variable(s) " + builder.toString());
        }
        return current;
    }

    private void handlePostWithBody() {
        var jsonParams = "";
        if (requestJSONObject != null) {
            jsonParams = fillEvaluationsInJson();
        }

        if (basicAuth == null) {
            try {
                rc.postBodyToEndpoint(this.getStoryName(), this, new URL(this.addr), jsonParams);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                log.error(e);
            }
        } else {
            try {

                rc.postBodyToEndpointWithAuth(this.getStoryName(), this, new URL(this.addr), jsonParams, getKnownParams().get(basicAuth.getUser()),
                        getKnownParams().get(basicAuth.getPassword()))
                ;
            } catch (IOException e) {
                // TODO Auto-generated catch block
                log.error(e);
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
                rc.putFormToEndpoint(this.getStoryName(), this, new URL(this.addr), params);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                log.error(e);
            }
        } else {
            try {

                rc.putFormToEndpointWithAuth(this.getStoryName(), this, new URL(this.addr), params, getKnownParams().get(basicAuth.getUser()),
                        getKnownParams().get(basicAuth.getPassword()))
                ;
            } catch (IOException e) {
                // TODO Auto-generated catch block
                log.error(e);
            }
        }
    }

    private void handlePutWithBody() {
        var jsonParams = "";
        if (requestJSONObject != null) {
            jsonParams = fillEvaluationsInJson();
        }

        if (basicAuth == null) {
            try {
                rc.putBodyToEndpoint(this.getStoryName(), this, new URL(this.addr), jsonParams);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                log.error(e);
            }
        } else {
            try {

                rc.putBodyToEndpointWithAuth(this.getStoryName(), this, new URL(this.addr), jsonParams, getKnownParams().get(basicAuth.getUser()),
                        getKnownParams().get(basicAuth.getPassword()))
                ;
            } catch (IOException e) {
                // TODO Auto-generated catch block
                log.error(e);
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
                rc.deleteFromEndpoint(this.getStoryName(), this, new URL(this.addr), params);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                log.error(e);
            }
        } else {
            try {
                rc.deleteFromEndpointWithAuth(this.getStoryName(), this, new URL(this.addr), params, getKnownParams().get(basicAuth.getUser()),
                        getKnownParams().get(basicAuth.getPassword()));
            } catch (IOException e) {
                // TODO Auto-generated catch block
                log.error(e);
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
                rc.getFromEndpoint(this.getStoryName(), this, new URL(this.addr), params);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                log.error(e);
            }
        } else {
            try {

                rc.getFromEndpointWithAuth(this.getStoryName(), this, new URL(this.addr), params, getKnownParams().get(basicAuth.getUser()),
                        getKnownParams().get(basicAuth.getPassword()))
                ;
            } catch (IOException e) {
                // TODO Auto-generated catch block
                log.error(e);
            }
        }
    }

    private void handleGetWithBody() {
        var jsonParams = "";
        if (requestJSONObject != null) {
            jsonParams = fillEvaluationsInJson();
        }

        if (basicAuth == null) {
            try {
                rc.getBodyFromEndpoint(this.getStoryName(), this, new URL(this.addr), jsonParams);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                log.error(e);
            }
        } else {
            try {

                rc.getBodyFromEndpointWithAuth(this.getStoryName(), this, new URL(this.addr), jsonParams, getKnownParams().get(basicAuth.getUser()),
                        getKnownParams().get(basicAuth.getPassword()))
                ;
            } catch (IOException e) {
                // TODO Auto-generated catch block
                log.error(e);
            }
        }
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BasicAuth {
        private String user;
        private String password;
    }
}
