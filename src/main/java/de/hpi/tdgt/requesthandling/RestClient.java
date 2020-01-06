package de.hpi.tdgt.requesthandling;

import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;
import de.hpi.tdgt.test.Test;
import de.hpi.tdgt.test.time_measurement.TimeStorage;
import lombok.extern.log4j.Log4j2;
import lombok.val;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.*;
import org.apache.http.entity.ByteArrayEntity;

import javax.annotation.Nullable;
import java.io.*;
import java.net.*;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Log4j2
public class RestClient {

    public RestResult getFromEndpoint(String story, URL url, Map<String, String> getParams) throws IOException {
        val request = new Request();
        request.setUrl(url);
        request.setParams(getParams);
        request.setMethod(HttpConstants.GET);
        request.setStory(story);
        return exchangeWithEndpoint(request);
    }

    public RestResult getBodyFromEndpoint(String story, URL url, String body) throws IOException {
        val request = new Request();
        request.setUrl(url);
        request.setMethod(HttpConstants.GET);
        request.setBody(body);
        request.setForm(false);
        request.setStory(story);
        return exchangeWithEndpoint(request);
    }

    public RestResult postFormToEndpoint(String story, URL url, Map<String, String> getParams) throws IOException {
        val request = new Request();
        request.setUrl(url);
        request.setParams(getParams);
        request.setMethod(HttpConstants.POST);
        request.setForm(true);
        request.setStory(story);
        return exchangeWithEndpoint(request);
    }

    public RestResult postBodyToEndpoint(String story, URL url, String body) throws IOException {
        val request = new Request();
        request.setUrl(url);
        request.setMethod(HttpConstants.POST);
        request.setForm(false);
        request.setBody(body);
        request.setStory(story);
        return exchangeWithEndpoint(request);
    }

    public RestResult putFormToEndpoint(String story, URL url, Map<String, String> getParams) throws IOException {
        val request = new Request();
        request.setUrl(url);
        request.setParams(getParams);
        request.setMethod(HttpConstants.PUT);
        request.setForm(true);
        request.setStory(story);
        return exchangeWithEndpoint(request);
    }

    public RestResult putBodyToEndpoint(String story, URL url, String body) throws IOException {
        val request = new Request();
        request.setUrl(url);
        request.setMethod(HttpConstants.PUT);
        request.setForm(false);
        request.setBody(body);
        request.setStory(story);
        return exchangeWithEndpoint(request);
    }

    public RestResult getFromEndpointWithAuth(String story, URL url, Map<String, String> getParams, String username, String password) throws IOException {
        val request = new Request();
        request.setUrl(url);
        request.setParams(getParams);
        request.setMethod(HttpConstants.GET);
        request.setUsername(username);
        request.setPassword(password);
        request.setStory(story);
        return exchangeWithEndpoint(request);
    }

    public RestResult getBodyFromEndpointWithAuth(String story, URL url, String body, String username, String password) throws IOException {
        val request = new Request();
        request.setUrl(url);
        request.setMethod(HttpConstants.GET);
        request.setBody(body);
        request.setForm(false);
        request.setUsername(username);
        request.setPassword(password);
        request.setStory(story);
        return exchangeWithEndpoint(request);
    }

    public RestResult postFormToEndpointWithAuth(String story, URL url, Map<String, String> getParams, String username, String password) throws IOException {
        val request = new Request();
        request.setUrl(url);
        request.setParams(getParams);
        request.setMethod(HttpConstants.POST);
        request.setForm(true);
        request.setUsername(username);
        request.setPassword(password);
        request.setStory(story);
        return exchangeWithEndpoint(request);
    }

    public RestResult postBodyToEndpointWithAuth(String story, URL url, String body, String username, String password) throws IOException {
        val request = new Request();
        request.setUrl(url);
        request.setMethod(HttpConstants.POST);
        request.setForm(false);
        request.setBody(body);
        request.setUsername(username);
        request.setPassword(password);
        request.setStory(story);
        return exchangeWithEndpoint(request);
    }

    public RestResult putFormToEndpointWithAuth(String story, URL url, Map<String, String> getParams, String username, String password) throws IOException {
        val request = new Request();
        request.setUrl(url);
        request.setParams(getParams);
        request.setMethod(HttpConstants.PUT);
        request.setForm(true);
        request.setUsername(username);
        request.setPassword(password);
        request.setStory(story);
        return exchangeWithEndpoint(request);
    }

    public RestResult putBodyToEndpointWithAuth(String story, URL url, String body, String username, String password) throws IOException {
        val request = new Request();
        request.setUrl(url);
        request.setMethod(HttpConstants.PUT);
        request.setForm(false);
        request.setBody(body);
        request.setUsername(username);
        request.setPassword(password);
        request.setStory(story);
        return exchangeWithEndpoint(request);
    }

    public RestResult deleteFromEndpoint(String story, URL url, Map<String, String> getParams) throws IOException {
        val request = new Request();
        request.setUrl(url);
        request.setParams(getParams);
        request.setMethod(HttpConstants.DELETE);
        return exchangeWithEndpoint(request);
    }

    public RestResult deleteFromEndpointWithAuth(String story, URL url, Map<String, String> getParams, String username, String password) throws IOException {
        val request = new Request();
        request.setUrl(url);
        request.setParams(getParams);
        request.setMethod(HttpConstants.DELETE);
        request.setUsername(username);
        request.setPassword(password);
        request.setStory(story);
        return exchangeWithEndpoint(request);
    }
    //above methods are for user's convenience, this method does the actual request
    public RestResult exchangeWithEndpoint(Request request) throws IOException {
        //need one instance per call, because has to altered (follows redirects)
        final OkHttpClient client = new OkHttpClient();
        //append GET parameters if necessary
        URL url = appendGetParametersToUrlIfNecessary(request.getUrl(), request.getParams(), request.getMethod());

        //set given properties
        prepareHttpClient(client, request.isFollowsRedirects(), request.getConnectTimeout(), request.getResponseTimeout(), request.isSendKeepAlive());
        val message = new com.squareup.okhttp.Request.Builder();
        message.url(url);
        int retry;
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
            message.method(request.getMethod(), RequestBody.create(MediaType.parse(HttpConstants.APPLICATION_X_WWW_FORM_URLENCODED), body) );
        }
        //set POST body to what was passed
        if (!request.isForm() && request.getBody() != null) {
            message.method(request.getMethod(), RequestBody.create(MediaType.parse(HttpConstants.CONTENT_TYPE_APPLICATION_JSON), request.getBody()) );
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
        Response response = null;
        //try to connect
        val start = System.nanoTime();
        for (retry = -1; retry < request.getRetries(); retry++) {
            try {
                response = client.newCall(message.build()).execute();
                break;
            } catch (SocketTimeoutException s) {
                log.warn("Request timeout for URL " + url.toString() + " (connect timeout was " + request.getConnectTimeout() + ").");
            } catch (IOException e) {
                log.error("Could not connect to " + url.toString(), e);
                return null;
            }
        }
        //exceeded max retries
        if (retry >= request.getRetries()) {
            return null;
        }
        //got a connection
        val result = new RestResult();
        result.setStartTime(start);
        readResponse(response, result, request);
        return result;
    }

    private void prepareHttpClient(OkHttpClient client, boolean followsRedirects, int connectTimeout, int responseTimeout, boolean sendKeepAlive) throws IOException {

        client.setFollowRedirects(followsRedirects);
        client.setFollowSslRedirects(followsRedirects);
        if (connectTimeout > 0) {
            client.setConnectTimeout(connectTimeout, TimeUnit.MILLISECONDS);
        }
        if (responseTimeout > 0) {
            client.setReadTimeout(connectTimeout, TimeUnit.MILLISECONDS);
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

    private static final byte[] NULL_BA = new byte[0];// can share these
    private static final TimeStorage storage = TimeStorage.getInstance();
    /**
     * Taken from jmeter.
     * Reads the response from the URL connection.
     *
     * @param response http response
     * @param res  {@link RestResult} to read response into
     * @throws IOException if an I/O exception occurs
     */
    private void readResponse(@Nullable Response response, RestResult res, Request request) throws IOException {
        //got results, store them and the time
        if(response!=null) {
            res.setResponse(response.body().bytes());
            res.setContentType(response.body().contentType().toString());
            res.setHeaders(response.headers());
            res.setReturnCode(response.code());
        }
        res.setEndTime(System.nanoTime());
        storage.registerTime(request.getMethod(), request.getUrl().toString(), res.durationNanos(), request.getStory());
        log.info("Request took "+res.durationMillis()+" ms.");
    }
}
