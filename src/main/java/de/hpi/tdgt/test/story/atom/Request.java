package de.hpi.tdgt.test.story.atom;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
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
    public void perform() throws InterruptedException {
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
                extractResponseParams(rc.postFormToEndpoint(this.getParent().getName(), this.getParent().getParent().getTestId(), new URL(this.addr), params));
            } catch (MalformedURLException e) {
                // TODO Auto-generated catch block
                log.error(e);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                log.error(e);
            }
        } else {
            try {
                extractResponseParams(
                        rc.postFormToEndpointWithAuth(this.getParent().getName(), this.getParent().getParent().getTestId(), new URL(this.addr), params, getKnownParams().get(basicAuth.getUser()),
                                getKnownParams().get(basicAuth.getPassword()))
                );
            } catch (MalformedURLException e) {
                // TODO Auto-generated catch block
                log.error(e);
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

    private void extractResponseParams(final RestResult result)
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
            assertion.check(result, this.getParent().getParent().getTestId());
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
                extractResponseParams(rc.postBodyToEndpoint(this.getParent().getName(), this.getParent().getParent().getTestId(), new URL(this.addr), jsonParams));
            } catch (MalformedURLException e) {
                // TODO Auto-generated catch block
                log.error(e);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                log.error(e);
            }
        } else {
            try {
                extractResponseParams(
                        rc.postBodyToEndpointWithAuth(this.getParent().getName(), this.getParent().getParent().getTestId(), new URL(this.addr), jsonParams, getKnownParams().get(basicAuth.getUser()),
                                getKnownParams().get(basicAuth.getPassword()))
                );
            } catch (MalformedURLException e) {
                // TODO Auto-generated catch block
                log.error(e);
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
                extractResponseParams(rc.putFormToEndpoint(this.getParent().getName(), this.getParent().getParent().getTestId(), new URL(this.addr), params));
            } catch (MalformedURLException e) {
                // TODO Auto-generated catch block
                log.error(e);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                log.error(e);
            }
        } else {
            try {
                extractResponseParams(
                        rc.putFormToEndpointWithAuth(this.getParent().getName(), this.getParent().getParent().getTestId(), new URL(this.addr), params, getKnownParams().get(basicAuth.getUser()),
                                getKnownParams().get(basicAuth.getPassword()))
                );
            } catch (MalformedURLException e) {
                // TODO Auto-generated catch block
                log.error(e);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                log.error(e);
            }
        }
    }

    private void handlePutWithBody() {
        val params = new HashMap<String, String>();
        if (requestJSONObject != null) {
            //fill out template
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
                extractResponseParams(rc.putBodyToEndpoint(this.getParent().getName(), this.getParent().getParent().getTestId(), new URL(this.addr), jsonParams));
            } catch (MalformedURLException e) {
                // TODO Auto-generated catch block
                log.error(e);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                log.error(e);
            }
        } else {
            try {
                extractResponseParams(
                        rc.putBodyToEndpointWithAuth(this.getParent().getName(), this.getParent().getParent().getTestId(), new URL(this.addr), jsonParams, getKnownParams().get(basicAuth.getUser()),
                                getKnownParams().get(basicAuth.getPassword()))
                );
            } catch (MalformedURLException e) {
                // TODO Auto-generated catch block
                log.error(e);
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
                extractResponseParams(rc.deleteFromEndpoint(this.getParent().getName(), this.getParent().getParent().getTestId(), new URL(this.addr), params));
            } catch (MalformedURLException e) {
                // TODO Auto-generated catch block
                log.error(e);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                log.error(e);
            }
        } else {
            try {
                extractResponseParams(rc.deleteFromEndpointWithAuth(this.getParent().getName(), this.getParent().getParent().getTestId(), new URL(this.addr), params, getKnownParams().get(basicAuth.getUser()),
                        getKnownParams().get(basicAuth.getPassword())));
            } catch (MalformedURLException e) {
                // TODO Auto-generated catch block
                log.error(e);
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
                extractResponseParams(rc.getFromEndpoint(this.getParent().getName(), this.getParent().getParent().getTestId(), new URL(this.addr), params));
            } catch (MalformedURLException e) {
                // TODO Auto-generated catch block
                log.error(e);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                log.error(e);
            }
        } else {
            try {
                extractResponseParams(
                        rc.getFromEndpointWithAuth(this.getParent().getName(), this.getParent().getParent().getTestId(), new URL(this.addr), params, getKnownParams().get(basicAuth.getUser()),
                                getKnownParams().get(basicAuth.getPassword()))
                );
            } catch (MalformedURLException e) {
                // TODO Auto-generated catch block
                log.error(e);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                log.error(e);
            }
        }
    }

    private void handleGetWithBody() {
        val params = new HashMap<String, String>();
        if (requestJSONObject != null) {
            //fill out template
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
                extractResponseParams(rc.getBodyFromEndpoint(this.getParent().getName(), this.getParent().getParent().getTestId(), new URL(this.addr), jsonParams));
            } catch (MalformedURLException e) {
                // TODO Auto-generated catch block
                log.error(e);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                log.error(e);
            }
        } else {
            try {
                extractResponseParams(
                        rc.getBodyFromEndpointWithAuth(this.getParent().getName(), this.getParent().getParent().getTestId(), new URL(this.addr), jsonParams, getKnownParams().get(basicAuth.getUser()),
                                getKnownParams().get(basicAuth.getPassword()))
                );
            } catch (MalformedURLException e) {
                // TODO Auto-generated catch block
                log.error(e);
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
