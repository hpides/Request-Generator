package de.hpi.tdgt.requesthandling;

import de.esoco.coroutine.ChannelId;
import de.esoco.coroutine.Coroutine;
import de.esoco.coroutine.step.Delay;
import de.hpi.tdgt.fibers.Scheduler;
import de.hpi.tdgt.test.Test;
import de.hpi.tdgt.test.time_measurement.TimeStorage;
import io.netty.handler.codec.http.HttpHeaders;
import lombok.extern.log4j.Log4j2;
import lombok.val;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.CountingInputStream;
import org.asynchttpclient.*;

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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.GZIPInputStream;

import static de.esoco.coroutine.ChannelId.stringChannel;
import static de.esoco.coroutine.step.ChannelReceive.receive;
import static de.esoco.coroutine.step.ChannelSend.send;
import static de.esoco.coroutine.step.CodeExecution.apply;
import static de.esoco.coroutine.step.CodeExecution.run;

@Log4j2
public class RestClient {

    public RestResult getFromEndpoint(String story, long testId,  URL url, Map<String, String> getParams) throws IOException {
        val request = new Request();
        request.setUrl(url);
        request.setParams(getParams);
        request.setMethod(HttpConstants.GET);
        request.setStory(story);
        request.setTestId(testId);
        return exchangeWithEndpoint(request);
    }

    public RestResult getBodyFromEndpoint(String story, long testId,  URL url, String body) throws IOException {
        val request = new Request();
        request.setUrl(url);
        request.setMethod(HttpConstants.GET);
        request.setBody(body);
        request.setForm(false);
        request.setStory(story);
        request.setTestId(testId);
        return exchangeWithEndpoint(request);
    }

    public RestResult postFormToEndpoint(String story, long testId,  URL url, Map<String, String> getParams) throws IOException {
        val request = new Request();
        request.setUrl(url);
        request.setParams(getParams);
        request.setMethod(HttpConstants.POST);
        request.setForm(true);
        request.setStory(story);
        request.setTestId(testId);
        return exchangeWithEndpoint(request);
    }

    public RestResult postBodyToEndpoint(String story, long testId,  URL url, String body) throws IOException {
        val request = new Request();
        request.setUrl(url);
        request.setMethod(HttpConstants.POST);
        request.setForm(false);
        request.setBody(body);
        request.setStory(story);
        request.setTestId(testId);
        return exchangeWithEndpoint(request);
    }

    public RestResult putFormToEndpoint(String story, long testId,  URL url, Map<String, String> getParams) throws IOException {
        val request = new Request();
        request.setUrl(url);
        request.setParams(getParams);
        request.setMethod(HttpConstants.PUT);
        request.setForm(true);
        request.setStory(story);
        request.setTestId(testId);
        return exchangeWithEndpoint(request);
    }

    public RestResult putBodyToEndpoint(String story, long testId,  URL url, String body) throws IOException {
        val request = new Request();
        request.setUrl(url);
        request.setMethod(HttpConstants.PUT);
        request.setForm(false);
        request.setBody(body);
        request.setStory(story);
        request.setTestId(testId);
        return exchangeWithEndpoint(request);
    }

    public RestResult getFromEndpointWithAuth(String story, long testId,  URL url, Map<String, String> getParams, String username, String password) throws IOException {
        val request = new Request();
        request.setUrl(url);
        request.setParams(getParams);
        request.setMethod(HttpConstants.GET);
        request.setUsername(username);
        request.setPassword(password);
        request.setStory(story);
        request.setTestId(testId);
        return exchangeWithEndpoint(request);
    }

    public RestResult getBodyFromEndpointWithAuth(String story, long testId,  URL url, String body, String username, String password) throws IOException {
        val request = new Request();
        request.setUrl(url);
        request.setMethod(HttpConstants.GET);
        request.setBody(body);
        request.setForm(false);
        request.setUsername(username);
        request.setPassword(password);
        request.setStory(story);
        request.setTestId(testId);
        return exchangeWithEndpoint(request);
    }

    public RestResult postFormToEndpointWithAuth(String story, long testId,  URL url, Map<String, String> getParams, String username, String password) throws IOException {
        val request = new Request();
        request.setUrl(url);
        request.setParams(getParams);
        request.setMethod(HttpConstants.POST);
        request.setForm(true);
        request.setUsername(username);
        request.setPassword(password);
        request.setStory(story);
        request.setTestId(testId);
        return exchangeWithEndpoint(request);
    }

    public RestResult postBodyToEndpointWithAuth(String story, long testId,  URL url, String body, String username, String password) throws IOException {
        val request = new Request();
        request.setUrl(url);
        request.setMethod(HttpConstants.POST);
        request.setForm(false);
        request.setBody(body);
        request.setUsername(username);
        request.setPassword(password);
        request.setStory(story);
        request.setTestId(testId);
        return exchangeWithEndpoint(request);
    }

    public RestResult putFormToEndpointWithAuth(String story, long testId,  URL url, Map<String, String> getParams, String username, String password) throws IOException {
        val request = new Request();
        request.setUrl(url);
        request.setParams(getParams);
        request.setMethod(HttpConstants.PUT);
        request.setForm(true);
        request.setUsername(username);
        request.setPassword(password);
        request.setStory(story);
        request.setTestId(testId);
        return exchangeWithEndpoint(request);
    }

    public RestResult putBodyToEndpointWithAuth(String story, long testId,  URL url, String body, String username, String password) throws IOException {
        val request = new Request();
        request.setUrl(url);
        request.setMethod(HttpConstants.PUT);
        request.setForm(false);
        request.setBody(body);
        request.setUsername(username);
        request.setPassword(password);
        request.setStory(story);
        request.setTestId(testId);
        return exchangeWithEndpoint(request);
    }

    public RestResult deleteFromEndpoint(String story, long testId,  URL url, Map<String, String> getParams) throws IOException {
        val request = new Request();
        request.setUrl(url);
        request.setParams(getParams);
        request.setMethod(HttpConstants.DELETE);
        request.setTestId(testId);
        return exchangeWithEndpoint(request);
    }

    public RestResult deleteFromEndpointWithAuth(String story, long testId,  URL url, Map<String, String> getParams, String username, String password) throws IOException {
        val request = new Request();
        request.setUrl(url);
        request.setParams(getParams);
        request.setMethod(HttpConstants.DELETE);
        request.setUsername(username);
        request.setPassword(password);
        request.setStory(story);
        request.setTestId(testId);
        return exchangeWithEndpoint(request);
    }

    //above methods are for user's convenience, this method does the actual request
    public RestResult exchangeWithEndpoint(Request request) throws IOException {
        //append GET parameters if necessary
        URL url = appendGetParametersToUrlIfNecessary(request.getUrl(), request.getParams(), request.getMethod());

        //set given properties
        val config = prepareHttpUrlConnection(url, request.getMethod(), request.isFollowsRedirects(), request.getConnectTimeout(), request.getResponseTimeout(), request.isSendKeepAlive());
        val client = Dsl.asyncHttpClient(config);
        int retry;
        val start = System.nanoTime();
        val requestBuilder = new RequestBuilder();
        requestBuilder.setUrl(url.toString());
        requestBuilder.setMethod(request.getMethod());
        BoundRequestBuilder builder = client.prepareRequest(requestBuilder);

        //set auth header if required
        if (request.getUsername() != null && request.getPassword() != null) {
            builder.setHeader(HttpConstants.HEADER_AUTHORIZATION, "Basic "+Base64.getEncoder().encodeToString((request.getUsername() + ":" + request.getPassword()).getBytes(StandardCharsets.UTF_8)));
        }

        //set POST Body to contain formencoded data
        if (request.isForm() && (request.getMethod().equals(HttpConstants.POST) || request.getMethod().equals(HttpConstants.PUT))) {
            builder.setHeader("Content-Type", HttpConstants.APPLICATION_X_WWW_FORM_URLENCODED);
            builder.setBody(mapToURLEncodedString(request.getParams()).toString());
        }
        //set POST body to what was passed
        if (!request.isForm() && (request.getMethod().equals(HttpConstants.POST) || request.getMethod().equals(HttpConstants.PUT) || request.getMethod().equals(HttpConstants.GET)) && request.getBody() != null) {
            builder.setHeader("Content-Type", HttpConstants.CONTENT_TYPE_APPLICATION_JSON);
            builder.setBody(request.getBody());
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
        val scope = Test.RequestThrottler.getInstance().getScope();
        val testChannel = stringChannel(url.toString()+System.nanoTime());
        //try to connect
        val result = new RestResult();
        final AtomicBoolean atomicBoolean = new AtomicBoolean(false);
        for (retry = -1; retry < request.getRetries(); retry++) {
            result.setStartTime(start);
            builder.execute(new AsyncCompletionHandler<RestResult>() {
                    @Override
                    public RestResult onCompleted(Response response) throws Exception {
                        readResponse(result, request, response);
                        atomicBoolean.set(true);
                        return result;
                    }
                });
            while(!atomicBoolean.get()) {
                Delay.sleep(1);
            }
                break;
        }
        //exceeded max retries
        if (retry >= request.getRetries()) {
            return null;
        }
        //got a connection
        return result;
    }

    private AsyncHttpClientConfig prepareHttpUrlConnection(URL url, String method, boolean followsRedirects, int connectTimeout, int responseTimeout, boolean sendKeepAlive) throws IOException {
        DefaultAsyncHttpClientConfig.Builder clientBuilder = Dsl.config();
        clientBuilder.setFollowRedirect(followsRedirects);
        clientBuilder.setKeepAlive(sendKeepAlive);
        if (connectTimeout > 0) {
            clientBuilder.setConnectTimeout(connectTimeout);
        }
        if (responseTimeout > 0) {

            clientBuilder.setReadTimeout(responseTimeout);
        }
        return clientBuilder.build();
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
     * @param conn URL from which to read response
     * @param res  {@link RestResult} to read response into
     * @return response content
     * @throws IOException if an I/O exception occurs
     */
    private void readResponse(RestResult res, Request request, Response response) throws IOException {
        res.setResponse(response.getResponseBodyAsBytes());
        res.setEndTime(System.nanoTime());
        res.setContentType(response.getContentType());
        res.setHeaders(response.getHeaders());
        res.setReturnCode(response.getStatusCode());
        storage.registerTime(request.getMethod(), request.getUrl().toString(), res.durationNanos(), request.getStory(), request.getTestId());
        log.info("Request took "+res.durationMillis()+" ms.");
    }
}
