package de.hpi.tdgt.requesthandling;

import de.hpi.tdgt.test.Test;
import de.hpi.tdgt.test.time_measurement.TimeStorage;
import lombok.extern.log4j.Log4j2;
import lombok.val;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.CountingInputStream;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.*;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.AbstractHttpMessage;

import javax.annotation.Nullable;
import java.io.*;
import java.net.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.Time;
import java.util.Base64;
import java.util.Map;
import java.util.zip.GZIPInputStream;

@Log4j2
public class RestClient {

    public RestResult getFromEndpoint(String story, URL url, Map<String, String> getParams) throws IOException {
        val request = new Request();
        request.setUrl(url);
        request.setParams(getParams);
        request.setMethod(HttpConstants.GET);
        request.setStory(story);
        return exchangeWithEndpoint(request, new HttpGet(appendGetParametersToUrlIfNecessary(url,getParams,HttpConstants.GET).toString()));
    }

    public RestResult getBodyFromEndpoint(String story, URL url, String body) throws IOException {
        val request = new Request();
        request.setUrl(url);
        request.setMethod(HttpConstants.GET);
        request.setBody(body);
        request.setForm(false);
        request.setStory(story);
        return exchangeWithEndpoint(request, new HttpGet(url.toString()));
    }

    public RestResult postFormToEndpoint(String story, URL url, Map<String, String> getParams) throws IOException {
        val request = new Request();
        request.setUrl(url);
        request.setParams(getParams);
        request.setMethod(HttpConstants.POST);
        request.setForm(true);
        request.setStory(story);
        return exchangeWithEndpoint(request, new HttpPost(url.toString()));
    }

    public RestResult postBodyToEndpoint(String story, URL url, String body) throws IOException {
        val request = new Request();
        request.setUrl(url);
        request.setMethod(HttpConstants.POST);
        request.setForm(false);
        request.setBody(body);
        request.setStory(story);
        return exchangeWithEndpoint(request, new HttpPost(url.toString()));
    }

    public RestResult putFormToEndpoint(String story, URL url, Map<String, String> getParams) throws IOException {
        val request = new Request();
        request.setUrl(url);
        request.setParams(getParams);
        request.setMethod(HttpConstants.PUT);
        request.setForm(true);
        request.setStory(story);
        return exchangeWithEndpoint(request, new HttpPut(url.toString()));
    }

    public RestResult putBodyToEndpoint(String story, URL url, String body) throws IOException {
        val request = new Request();
        request.setUrl(url);
        request.setMethod(HttpConstants.PUT);
        request.setForm(false);
        request.setBody(body);
        request.setStory(story);
        return exchangeWithEndpoint(request, new HttpPut(url.toString()));
    }

    public RestResult getFromEndpointWithAuth(String story, URL url, Map<String, String> getParams, String username, String password) throws IOException {
        val request = new Request();
        request.setUrl(url);
        request.setParams(getParams);
        request.setMethod(HttpConstants.GET);
        request.setUsername(username);
        request.setPassword(password);
        request.setStory(story);
        return exchangeWithEndpoint(request, new HttpGet(appendGetParametersToUrlIfNecessary(url, getParams, HttpConstants.GET).toString()));
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
        return exchangeWithEndpoint(request, new HttpGet(url.toString()));
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
        return exchangeWithEndpoint(request, new HttpPost(url.toString()));
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
        return exchangeWithEndpoint(request, new HttpPost(url.toString()));
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
        return exchangeWithEndpoint(request, new HttpPut(url.toString()));
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
        return exchangeWithEndpoint(request, new HttpPut(url.toString()));
    }

    public RestResult deleteFromEndpoint(String story, URL url, Map<String, String> getParams) throws IOException {
        val request = new Request();
        request.setUrl(url);
        request.setParams(getParams);
        request.setMethod(HttpConstants.DELETE);
        return exchangeWithEndpoint(request, new HttpDelete(appendGetParametersToUrlIfNecessary(url,getParams,HttpConstants.DELETE).toString()));
    }

    public RestResult deleteFromEndpointWithAuth(String story, URL url, Map<String, String> getParams, String username, String password) throws IOException {
        val request = new Request();
        request.setUrl(url);
        request.setParams(getParams);
        request.setMethod(HttpConstants.DELETE);
        request.setUsername(username);
        request.setPassword(password);
        request.setStory(story);
        return exchangeWithEndpoint(request, new HttpDelete(appendGetParametersToUrlIfNecessary(url,getParams,HttpConstants.DELETE).toString()));
    }
    private final HttpClient client = HttpClients.createDefault();
    //above methods are for user's convenience, this method does the actual request
    public RestResult exchangeWithEndpoint(Request request, HttpRequestBase message) throws IOException {
        //append GET parameters if necessary
        URL url = appendGetParametersToUrlIfNecessary(request.getUrl(), request.getParams(), request.getMethod());

        //set given properties
        val config = prepareHttpUrlConnection(request.isFollowsRedirects(), request.getConnectTimeout(), request.getResponseTimeout(), request.isSendKeepAlive());
        int retry;
        if(request.isSendKeepAlive()){
            message.setHeader(HttpConstants.HEADER_CONNECTION, HttpConstants.KEEP_ALIVE);
        }
        //set auth header if required
        if (request.getUsername() != null && request.getPassword() != null) {
            message.setHeader(HttpConstants.HEADER_AUTHORIZATION, "Basic "+Base64.getEncoder().encodeToString((request.getUsername() + ":" + request.getPassword()).getBytes(StandardCharsets.UTF_8)));
        }
        //set POST Body to contain formencoded data
        if (request.isForm() && message instanceof HttpEntityEnclosingRequestBase) {
            message.setHeader("Content-Type", HttpConstants.APPLICATION_X_WWW_FORM_URLENCODED);
            String body = mapToURLEncodedString(request.getParams()).toString();
            val messageRepr = (HttpEntityEnclosingRequestBase) message;
            messageRepr.setEntity(new ByteArrayEntity(body.getBytes(StandardCharsets.UTF_8)));
        }
        //set POST body to what was passed
        if (!request.isForm() && message instanceof HttpEntityEnclosingRequestBase && request.getBody() != null) {
            message.setHeader("Content-Type", HttpConstants.CONTENT_TYPE_APPLICATION_JSON);
            val messageRepr = (HttpEntityEnclosingRequestBase) message;
            messageRepr.setEntity(new ByteArrayEntity(request.getBody().getBytes(StandardCharsets.UTF_8)));
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
        HttpResponse response = null;
        //try to connect
        val start = System.nanoTime();
        for (retry = -1; retry < request.getRetries(); retry++) {
            try {
                response = client.execute(message);
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

    private RequestConfig prepareHttpUrlConnection(boolean followsRedirects, int connectTimeout, int responseTimeout, boolean sendKeepAlive) throws IOException {
        final RequestConfig.Builder config = RequestConfig.custom();
        config.setRedirectsEnabled(followsRedirects);
        if (connectTimeout > 0) {
            config.setConnectTimeout(connectTimeout);
        }
        if (responseTimeout > 0) {
            config.setSocketTimeout(responseTimeout);
        }
        return config.build();
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
    private void readResponse(@Nullable HttpResponse response, RestResult res, Request request) throws IOException {
        //got results, store them and the time
        if(response!=null) {
            res.setResponse(response.getEntity().getContent().readAllBytes());
            res.setContentType(response.getEntity().getContentType().getValue());
            res.setHeaders(response.getAllHeaders());
            res.setReturnCode(response.getStatusLine().getStatusCode());
        }
        res.setEndTime(System.nanoTime());
        storage.registerTime(request.getMethod(), request.getUrl().toString(), res.durationNanos(), request.getStory());
        log.info("Request took "+res.durationMillis()+" ms.");
    }
}
