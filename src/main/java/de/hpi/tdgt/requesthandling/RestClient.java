package de.hpi.tdgt.requesthandling;

import de.hpi.tdgt.test.Test;
import de.hpi.tdgt.test.time_measurement.TimeStorage;
import lombok.extern.log4j.Log4j2;
import lombok.val;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.CountingInputStream;

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

    public RestResult getFromEndpoint(URL url, Map<String, String> getParams) throws IOException {
        val request = new Request();
        request.setUrl(url);
        request.setParams(getParams);
        request.setMethod(HttpConstants.GET);
        return exchangeWithEndpoint(request);
    }

    public RestResult getBodyFromEndpoint(URL url, String body) throws IOException {
        val request = new Request();
        request.setUrl(url);
        request.setMethod(HttpConstants.GET);
        request.setBody(body);
        request.setForm(false);
        return exchangeWithEndpoint(request);
    }

    public RestResult postFormToEndpoint(URL url, Map<String, String> getParams) throws IOException {
        val request = new Request();
        request.setUrl(url);
        request.setParams(getParams);
        request.setMethod(HttpConstants.POST);
        request.setForm(true);
        return exchangeWithEndpoint(request);
    }

    public RestResult postBodyToEndpoint(URL url, String body) throws IOException {
        val request = new Request();
        request.setUrl(url);
        request.setMethod(HttpConstants.POST);
        request.setForm(false);
        request.setBody(body);
        return exchangeWithEndpoint(request);
    }

    public RestResult putFormToEndpoint(URL url, Map<String, String> getParams) throws IOException {
        val request = new Request();
        request.setUrl(url);
        request.setParams(getParams);
        request.setMethod(HttpConstants.PUT);
        request.setForm(true);
        return exchangeWithEndpoint(request);
    }

    public RestResult putBodyToEndpoint(URL url, String body) throws IOException {
        val request = new Request();
        request.setUrl(url);
        request.setMethod(HttpConstants.PUT);
        request.setForm(false);
        request.setBody(body);
        return exchangeWithEndpoint(request);
    }

    public RestResult getFromEndpointWithAuth(URL url, Map<String, String> getParams, String username, String password) throws IOException {
        val request = new Request();
        request.setUrl(url);
        request.setParams(getParams);
        request.setMethod(HttpConstants.GET);
        request.setUsername(username);
        request.setPassword(password);
        return exchangeWithEndpoint(request);
    }

    public RestResult getBodyFromEndpointWithAuth(URL url, String body, String username, String password) throws IOException {
        val request = new Request();
        request.setUrl(url);
        request.setMethod(HttpConstants.GET);
        request.setBody(body);
        request.setForm(false);
        request.setUsername(username);
        request.setPassword(password);
        return exchangeWithEndpoint(request);
    }

    public RestResult postFormToEndpointWithAuth(URL url, Map<String, String> getParams, String username, String password) throws IOException {
        val request = new Request();
        request.setUrl(url);
        request.setParams(getParams);
        request.setMethod(HttpConstants.POST);
        request.setForm(true);
        request.setUsername(username);
        request.setPassword(password);
        return exchangeWithEndpoint(request);
    }

    public RestResult postBodyToEndpointWithAuth(URL url, String body, String username, String password) throws IOException {
        val request = new Request();
        request.setUrl(url);
        request.setMethod(HttpConstants.POST);
        request.setForm(false);
        request.setBody(body);
        request.setUsername(username);
        request.setPassword(password);
        return exchangeWithEndpoint(request);
    }

    public RestResult putFormToEndpointWithAuth(URL url, Map<String, String> getParams, String username, String password) throws IOException {
        val request = new Request();
        request.setUrl(url);
        request.setParams(getParams);
        request.setMethod(HttpConstants.PUT);
        request.setForm(true);
        request.setUsername(username);
        request.setPassword(password);
        return exchangeWithEndpoint(request);
    }

    public RestResult putBodyToEndpointWithAuth(URL url, String body, String username, String password) throws IOException {
        val request = new Request();
        request.setUrl(url);
        request.setMethod(HttpConstants.PUT);
        request.setForm(false);
        request.setBody(body);
        request.setUsername(username);
        request.setPassword(password);
        return exchangeWithEndpoint(request);
    }

    public RestResult deleteFromEndpoint(URL url, Map<String, String> getParams) throws IOException {
        val request = new Request();
        request.setUrl(url);
        request.setParams(getParams);
        request.setMethod(HttpConstants.DELETE);
        return exchangeWithEndpoint(request);
    }

    public RestResult deleteFromEndpointWithAuth(URL url, Map<String, String> getParams, String username, String password) throws IOException {
        val request = new Request();
        request.setUrl(url);
        request.setParams(getParams);
        request.setMethod(HttpConstants.DELETE);
        request.setUsername(username);
        request.setPassword(password);
        return exchangeWithEndpoint(request);
    }

    //above methods are for user's convenience, this method does the actual request
    public RestResult exchangeWithEndpoint(Request request) throws IOException {
        //append GET parameters if necessary
        URL url = appendGetParametersToUrlIfNecessary(request.getUrl(), request.getParams(), request.getMethod());

        //set given properties
        final HttpURLConnection httpURLConnection = prepareHttpUrlConnection(url, request.getMethod(), request.isFollowsRedirects(), request.getConnectTimeout(), request.getResponseTimeout(), request.isSendKeepAlive());
        int retry;
        val start = System.nanoTime();
        //set auth header if required
        if (request.getUsername() != null && request.getPassword() != null) {
            httpURLConnection.setRequestProperty(HttpConstants.HEADER_AUTHORIZATION, "Basic "+Base64.getEncoder().encodeToString((request.getUsername() + ":" + request.getPassword()).getBytes(StandardCharsets.UTF_8)));
        }
        //set POST Body to contain formencoded data
        if (request.isForm() && (request.getMethod().equals(HttpConstants.POST) || request.getMethod().equals(HttpConstants.PUT))) {
            httpURLConnection.setRequestProperty("Content-Type", HttpConstants.APPLICATION_X_WWW_FORM_URLENCODED);
            String body = mapToURLEncodedString(request.getParams()).toString();
            httpURLConnection.setDoOutput(true);
            OutputStream out = httpURLConnection.getOutputStream();
            out.write(body.getBytes(StandardCharsets.UTF_8));
            out.flush();
            out.close();
        }
        //set POST body to what was passed
        if (!request.isForm() && (request.getMethod().equals(HttpConstants.POST) || request.getMethod().equals(HttpConstants.PUT) || request.getMethod().equals(HttpConstants.GET)) && request.getBody() != null) {
            httpURLConnection.setRequestProperty("Content-Type", HttpConstants.CONTENT_TYPE_APPLICATION_JSON);
            httpURLConnection.setDoOutput(true);
            OutputStream out = httpURLConnection.getOutputStream();
            out.write(request.getBody().getBytes(StandardCharsets.UTF_8));
            out.flush();
            out.close();
        }
        /*if(Test.RequestThrottler.getInstance() != null) {
            Test.RequestThrottler.getInstance().allowRequest();
        }
        else {
            log.warn("Internal error: Can not limit requests per second!");
        }*/
        //try to connect
        for (retry = -1; retry < request.getRetries(); retry++) {
            try {
                httpURLConnection.connect();
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
        readResponse(httpURLConnection, result, request);

        return result;
    }

    private HttpURLConnection prepareHttpUrlConnection(URL url, String method, boolean followsRedirects, int connectTimeout, int responseTimeout, boolean sendKeepAlive) throws IOException {
        final HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
        httpURLConnection.setInstanceFollowRedirects(followsRedirects);
        if (connectTimeout > 0) {
            httpURLConnection.setConnectTimeout(connectTimeout);
        }
        if (responseTimeout > 0) {
            httpURLConnection.setReadTimeout(responseTimeout);
        }
        if (sendKeepAlive) {
            httpURLConnection.setRequestProperty(HttpConstants.HEADER_CONNECTION, HttpConstants.KEEP_ALIVE);
        }
        //TODO headers like content type
        httpURLConnection.setRequestMethod(method);
        return httpURLConnection;
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
    private void readResponse(HttpURLConnection conn, RestResult res, Request request) throws IOException {
        BufferedInputStream in = null;

        //final long contentLength = conn.getContentLength();
        //might return nullbyte here if we do not need actual content

        // works OK even if ContentEncoding is null
        boolean gzipped = HttpConstants.ENCODING_GZIP.equals(conn.getContentEncoding());
        CountingInputStream instream = null;
        try {
            instream = new CountingInputStream(conn.getInputStream());
            if (gzipped) {
                in = new BufferedInputStream(new GZIPInputStream(instream));
            } else {
                in = new BufferedInputStream(instream);
            }
        } catch (IOException e) {
            if (!(e.getCause() instanceof FileNotFoundException)) {
                Throwable cause = e.getCause();
                if (cause != null) {
                    //log.error("Cause: {}", cause.toString());
                    if (cause instanceof Error) {
                        throw (Error) cause;
                    }
                }
            }
            // Normal InputStream is not available
            InputStream errorStream = conn.getErrorStream();
            if (errorStream == null) {
                res.setResponse(NULL_BA);
                res.setEndTime(System.nanoTime());
                res.setContentType(conn.getContentType());
                res.setHeaders(conn.getHeaderFields());
                res.setReturnCode(conn.getResponseCode());
            }
            if (log.isInfoEnabled()) {
                if(conn.getResponseCode() == 401 || conn.getResponseCode() == 403){
                    log.info("Error Response Code: " + conn.getResponseCode() + "for "+request.getMethod() +" "+request.getUrl() + " for used authentication "+request.getUsername()+":"+request.getPassword());
                }
                else{
                    log.info("Error Response Code: " + conn.getResponseCode() + "for "+request.getMethod() +" "+request.getUrl());
                }
                if(errorStream != null){
                    log.warn("Error Response Content: ",IOUtils.toString(errorStream, "UTF-8") );
                }
            }

            if (gzipped) {
                if (errorStream != null) {
                    in = new BufferedInputStream(new GZIPInputStream(errorStream));
                }
            } else {
                if (errorStream != null) {
                    in = new BufferedInputStream(errorStream);
                }
            }
        } catch (Exception e) {
            Throwable cause = e.getCause();
            if (cause != null) {
                if (cause instanceof Error) {
                    throw (Error) cause;
                }
            }
            in = new BufferedInputStream(conn.getErrorStream());
        }
        // N.B. this closes 'in'
        var responseData = NULL_BA;
        if (in != null) {
            responseData = IOUtils.toByteArray(in);
            in.close();
        }
        if (instream != null) {
            instream.close();
        }
        //got results, store them and the time
        //TODO do we want to measure the time to transfer the data? Currently we are, but we could also take the time after retrieving content length
        res.setResponse(responseData);
        res.setEndTime(System.nanoTime());
        res.setContentType(conn.getContentType());
        res.setHeaders(conn.getHeaderFields());
        res.setReturnCode(conn.getResponseCode());
        storage.registerTime(request.getMethod(), request.getUrl().toString(), res.durationNanos());
        log.info("Request took "+res.durationMillis()+" ms.");
    }
}
