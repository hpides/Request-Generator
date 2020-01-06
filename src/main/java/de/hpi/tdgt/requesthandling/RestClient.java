package de.hpi.tdgt.requesthandling;
import de.hpi.tdgt.test.Test;
import de.hpi.tdgt.test.time_measurement.TimeStorage;
import lombok.extern.log4j.Log4j2;
import lombok.val;
import org.asynchttpclient.*;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

@Log4j2
public class RestClient {

    public RestResult getFromEndpoint(String story, de.hpi.tdgt.test.story.atom.Request callbackRequest, URL url, Map<String, String> getParams) throws IOException {
        val request = new Request();
        request.setUrl(url);
        request.setParams(getParams);
        request.setMethod(HttpConstants.GET);
        request.setStory(story);
        return exchangeWithEndpoint(request, callbackRequest);
    }

    public RestResult getBodyFromEndpoint(String story, de.hpi.tdgt.test.story.atom.Request callbackRequest, URL url, String body) throws IOException {
        val request = new Request();
        request.setUrl(url);
        request.setMethod(HttpConstants.GET);
        request.setBody(body);
        request.setForm(false);
        request.setStory(story);
        return exchangeWithEndpoint(request, callbackRequest);
    }

    public RestResult postFormToEndpoint(String story, de.hpi.tdgt.test.story.atom.Request callbackRequest, URL url, Map<String, String> getParams) throws IOException {
        val request = new Request();
        request.setUrl(url);
        request.setParams(getParams);
        request.setMethod(HttpConstants.POST);
        request.setForm(true);
        request.setStory(story);
        return exchangeWithEndpoint(request, callbackRequest);
    }

    public RestResult postBodyToEndpoint(String story, de.hpi.tdgt.test.story.atom.Request callbackRequest, URL url, String body) throws IOException {
        val request = new Request();
        request.setUrl(url);
        request.setMethod(HttpConstants.POST);
        request.setForm(false);
        request.setBody(body);
        request.setStory(story);
        return exchangeWithEndpoint(request, callbackRequest);
    }

    public RestResult putFormToEndpoint(String story, de.hpi.tdgt.test.story.atom.Request callbackRequest, URL url, Map<String, String> getParams) throws IOException {
        val request = new Request();
        request.setUrl(url);
        request.setParams(getParams);
        request.setMethod(HttpConstants.PUT);
        request.setForm(true);
        request.setStory(story);
        return exchangeWithEndpoint(request, callbackRequest);
    }

    public RestResult putBodyToEndpoint(String story, de.hpi.tdgt.test.story.atom.Request callbackRequest, URL url, String body) throws IOException {
        val request = new Request();
        request.setUrl(url);
        request.setMethod(HttpConstants.PUT);
        request.setForm(false);
        request.setBody(body);
        request.setStory(story);
        return exchangeWithEndpoint(request, callbackRequest);
    }

    public RestResult getFromEndpointWithAuth(String story, de.hpi.tdgt.test.story.atom.Request callbackRequest, URL url, Map<String, String> getParams, String username, String password) throws IOException {
        val request = new Request();
        request.setUrl(url);
        request.setParams(getParams);
        request.setMethod(HttpConstants.GET);
        request.setUsername(username);
        request.setPassword(password);
        request.setStory(story);
        return exchangeWithEndpoint(request, callbackRequest);
    }

    public RestResult getBodyFromEndpointWithAuth(String story, de.hpi.tdgt.test.story.atom.Request callbackRequest, URL url, String body, String username, String password) throws IOException {
        val request = new Request();
        request.setUrl(url);
        request.setMethod(HttpConstants.GET);
        request.setBody(body);
        request.setForm(false);
        request.setUsername(username);
        request.setPassword(password);
        request.setStory(story);
        return exchangeWithEndpoint(request, callbackRequest);
    }

    public RestResult postFormToEndpointWithAuth(String story, de.hpi.tdgt.test.story.atom.Request callbackRequest, URL url, Map<String, String> getParams, String username, String password) throws IOException {
        val request = new Request();
        request.setUrl(url);
        request.setParams(getParams);
        request.setMethod(HttpConstants.POST);
        request.setForm(true);
        request.setUsername(username);
        request.setPassword(password);
        request.setStory(story);
        return exchangeWithEndpoint(request, callbackRequest);
    }

    public RestResult postBodyToEndpointWithAuth(String story, de.hpi.tdgt.test.story.atom.Request callbackRequest, URL url, String body, String username, String password) throws IOException {
        val request = new Request();
        request.setUrl(url);
        request.setMethod(HttpConstants.POST);
        request.setForm(false);
        request.setBody(body);
        request.setUsername(username);
        request.setPassword(password);
        request.setStory(story);
        return exchangeWithEndpoint(request, callbackRequest);
    }

    public RestResult putFormToEndpointWithAuth(String story, de.hpi.tdgt.test.story.atom.Request callbackRequest, URL url, Map<String, String> getParams, String username, String password) throws IOException {
        val request = new Request();
        request.setUrl(url);
        request.setParams(getParams);
        request.setMethod(HttpConstants.PUT);
        request.setForm(true);
        request.setUsername(username);
        request.setPassword(password);
        request.setStory(story);
        return exchangeWithEndpoint(request, callbackRequest);
    }

    public RestResult putBodyToEndpointWithAuth(String story, de.hpi.tdgt.test.story.atom.Request callbackRequest, URL url, String body, String username, String password) throws IOException {
        val request = new Request();
        request.setUrl(url);
        request.setMethod(HttpConstants.PUT);
        request.setForm(false);
        request.setBody(body);
        request.setUsername(username);
        request.setPassword(password);
        request.setStory(story);
        return exchangeWithEndpoint(request, callbackRequest);
    }

    public RestResult deleteFromEndpoint(String story, de.hpi.tdgt.test.story.atom.Request callbackRequest, URL url, Map<String, String> getParams) throws IOException {
        val request = new Request();
        request.setUrl(url);
        request.setParams(getParams);
        request.setMethod(HttpConstants.DELETE);
        request.setStory(story);
        return exchangeWithEndpoint(request, callbackRequest);
    }

    public RestResult deleteFromEndpointWithAuth(String story, de.hpi.tdgt.test.story.atom.Request callbackRequest, URL url, Map<String, String> getParams, String username, String password) throws IOException {
        val request = new Request();
        request.setUrl(url);
        request.setParams(getParams);
        request.setMethod(HttpConstants.DELETE);
        request.setUsername(username);
        request.setPassword(password);
        request.setStory(story);
        return exchangeWithEndpoint(request, callbackRequest);
    }

    /**
     * above methods are for user's convenience, this method does the actual request
     * Will do an asynchronous request if callbackRequest is not null, else a synchronous request.
     * If the callbackRequest is not null, it's method extractResponseParams will be called
     */
    private RestResult exchangeWithEndpoint(Request request, final de.hpi.tdgt.test.story.atom.Request callbackRequest) throws IOException {
        //need one instance per call, because has to altered (follows redirects)
        final DefaultAsyncHttpClientConfig.Builder client = Dsl.config();
        //append GET parameters if necessary
        URL url = appendGetParametersToUrlIfNecessary(request.getUrl(), request.getParams(), request.getMethod());

        //set given properties
        prepareHttpClient(client, request.isFollowsRedirects(), request.getConnectTimeout(), request.getResponseTimeout());
        val restclient = Dsl.asyncHttpClient(client);
        val message = new RequestBuilder(request.getMethod());
        message.setUrl(url.toString());
        if(request.isSendKeepAlive()){
            message.addHeader(HttpConstants.HEADER_CONNECTION, HttpConstants.KEEP_ALIVE);
        }
        //set auth header if required
        if (request.getUsername() != null && request.getPassword() != null) {
            message.addHeader(HttpConstants.HEADER_AUTHORIZATION, "Basic "+Base64.getEncoder().encodeToString((request.getUsername() + ":" + request.getPassword()).getBytes(StandardCharsets.UTF_8)));
        }
        //set POST Body to contain formencoded data
        if (request.isForm() && request.getBody() == null) {
            String body = mapToURLEncodedString(request.getParams()).toString();
            message.setMethod(request.getMethod());
            message.setBody(body);
            message.addHeader(HttpConstants.HEADER_CONTENT_TYPE, HttpConstants.APPLICATION_X_WWW_FORM_URLENCODED);
        }
        //set POST body to what was passed
        if (!request.isForm() && request.getBody() != null) {
            message.setBody(request.getBody());
            message.addHeader(HttpConstants.HEADER_CONTENT_TYPE, HttpConstants.CONTENT_TYPE_APPLICATION_JSON);
        }
        if(Test.RequestThrottler.getInstance() != null) {
            try {
                Test.RequestThrottler.getInstance().allowRequest();
            } catch (InterruptedException e) {
                log.error("Interrupted wail waiting to be allowed to send a request: ",e);
            }
        }
        else {
            log.warn("Internal error: Can not limit requests per second!");
        }
        Future<Response> response;
        //try to connect
        val start = System.nanoTime();
        val result = new RestResult();
        result.setStartTime(start);

        response = restclient.executeRequest(message.build(), new AsyncCompletionHandler<>() {
            @Override
            public Response onCompleted(Response response) throws Exception {
                //else default to synchronous operation
                if (callbackRequest != null) {
                    readResponse(response, result, request);
                    callbackRequest.extractResponseParams(result);
                }
                return response;
            }
        });


        //got a connection
        if(callbackRequest == null) {
            //no callback, so default to synchronous operation (e.g. used in tests)
            Response actualResponse = null;
            try {
                if (response != null) {
                    actualResponse = response.get();
                    restclient.close();
                }
            } catch (InterruptedException | ExecutionException e) {
                log.error(e);
            }
            readResponse(actualResponse, result, request);
        }
        return result;
    }

    private void prepareHttpClient(DefaultAsyncHttpClientConfig.Builder client, boolean followsRedirects, int connectTimeout, int responseTimeout) {

        client.setFollowRedirect(followsRedirects);
        if (connectTimeout > 0) {
            client.setConnectTimeout(connectTimeout);
        }
        if (responseTimeout > 0) {
            client.setReadTimeout(connectTimeout);
        }
    }

    private URL appendGetParametersToUrlIfNecessary(URL url, Map<String, String> params, String method) throws MalformedURLException {
        //add URL parameters
        if (((method.equals(HttpConstants.GET)) || method.equals(HttpConstants.DELETE)) && params != null && !params.isEmpty()) {
            url = new URL(url.toString() + "?" + mapToURLEncodedString(params));
        }

        return url;
    }

    private StringBuilder mapToURLEncodedString(Map<String, String> params) {
        val finalURL = new StringBuilder();
        if (params != null && !params.isEmpty()) {
            boolean firstParam = true;
            for (val key : params.entrySet()) {
                if (!firstParam) {
                    finalURL.append("&");
                }
                //error handling
                if(key.getValue() ==null){
                    log.error("No value!");
                    continue;
                }
                firstParam = false;
                finalURL.append(URLEncoder.encode(key.getKey(), StandardCharsets.UTF_8));
                finalURL.append("=");
                finalURL.append(URLEncoder.encode(key.getValue(), StandardCharsets.UTF_8));
            }
        }
        return finalURL;
    }
    private static final TimeStorage storage = TimeStorage.getInstance();
    /**
     * Taken from jmeter.
     * Reads the response from the URL connection.
     *
     * @param response http response
     * @param res  {@link RestResult} to read response into
     */
    private void readResponse(Response response, RestResult res, Request request) {
        //got results, store them and the time
        if(response!=null) {
            res.setResponse(response.getResponseBodyAsBytes());
            res.setContentType(response.getContentType());
            res.setHeaders(response.getHeaders());
            res.setReturnCode(response.getStatusCode());
        }
        res.setEndTime(System.nanoTime());
        storage.registerTime(request.getMethod(), request.getUrl().toString(), res.durationNanos(), request.getStory());
        log.info("Request took "+res.durationMillis()+" ms.");
    }
}
