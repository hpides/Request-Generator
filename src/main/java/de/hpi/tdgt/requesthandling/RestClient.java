package de.hpi.tdgt.requesthandling;

import lombok.extern.log4j.Log4j2;
import lombok.val;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.CountingInputStream;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
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

    /**
     * Post formencoded data to endpoint.
     * @param url
     * @param getParams
     * @return
     * @throws IOException
     */
    public RestResult postFormToEndpoint(URL url, Map<String, String> getParams) throws IOException {
        val request = new Request();
        request.setUrl(url);
        request.setParams(getParams);
        request.setMethod(HttpConstants.POST);
        request.setForm(true);
        return exchangeWithEndpoint(request);
    }

    /**
     * Post a body to an endpoint.
     * @param url
     * @param body
     * @return
     * @throws IOException
     */
    public RestResult postBodyToEndpoint(URL url, String body) throws IOException {
        val request = new Request();
        request.setUrl(url);
        request.setMethod(HttpConstants.POST);
        request.setForm(false);
        request.setBody(body);
        return exchangeWithEndpoint(request);
    }

    /**
     * Post formencoded data to endpoint.
     * @param url
     * @param getParams
     * @return
     * @throws IOException
     */
    public RestResult putFormToEndpoint(URL url, Map<String, String> getParams) throws IOException {
        val request = new Request();
        request.setUrl(url);
        request.setParams(getParams);
        request.setMethod(HttpConstants.PUT);
        request.setForm(true);
        return exchangeWithEndpoint(request);
    }

    /**
     * Post a body to an endpoint.
     * @param url
     * @param body
     * @return
     * @throws IOException
     */
    public RestResult putBodyToEndpoint(URL url, String body) throws IOException {
        val request = new Request();
        request.setUrl(url);
        request.setMethod(HttpConstants.PUT);
        request.setForm(false);
        request.setBody(body);
        return exchangeWithEndpoint(request);
    }




    public RestResult exchangeWithEndpoint(Request request) throws IOException {
        URL url = appendGetParametersToUrlIfNecessary(request.getUrl(), request.getParams(), request.getMethod());


        final HttpURLConnection httpURLConnection = prepareHttpUrlConnection(url, request.getMethod(), request.isFollowsRedirects(), request.getConnectTimeout(), request.getResponseTimeout(), request.isSendKeepAlive());
        int retry;
        if(request.isForm() && (request.getMethod().equals(HttpConstants.POST) || request.getMethod().equals(HttpConstants.PUT))){
            String body = mapToURLEncodedString(request.getParams()).toString();
            httpURLConnection.setDoOutput(true);
            OutputStream out = httpURLConnection.getOutputStream();
            out.write(body.getBytes(StandardCharsets.UTF_8));
            out.flush();
            out.close();
        }
        if(!request.isForm() && (request.getMethod().equals(HttpConstants.POST) || request.getMethod().equals(HttpConstants.PUT)) && request.getBody() != null){
            httpURLConnection.setDoOutput(true);
            OutputStream out = httpURLConnection.getOutputStream();
            out.write(request.getBody().getBytes(StandardCharsets.UTF_8));
            out.flush();
            out.close();
        }
        val start = System.nanoTime();
        for(retry = -1; retry < request.getRetries(); retry++){
            try{
                httpURLConnection.connect();
                break;
            }catch (SocketTimeoutException s){
                log.warn("Request timeout for URL "+url.toString()+" (connect timeout was "+request.getConnectTimeout()+").");
            }
            catch (IOException e){
                log.error("Could not connect to "+url.toString(), e);
                return null;
            }
        }
        //exceeded max retries
        if(retry >= request.getRetries()){
            return null;
        }

        val result = new RestResult();
        result.setStartTime(start);
        readResponse(httpURLConnection, result);

        return result;
    }

    private HttpURLConnection prepareHttpUrlConnection(URL url, String method, boolean followsRedirects, int connectTimeout, int responseTimeout, boolean sendKeepAlive) throws IOException {
        final HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
        httpURLConnection.setInstanceFollowRedirects(followsRedirects);
        if(connectTimeout > 0) {
            httpURLConnection.setConnectTimeout(connectTimeout);
        }
        if(responseTimeout > 0) {
            httpURLConnection.setReadTimeout(responseTimeout);
        }
        if(sendKeepAlive) {
            httpURLConnection.setRequestProperty(HttpConstants.HEADER_CONNECTION, HttpConstants.KEEP_ALIVE);
        }
        //TODO headers, especially auth
        httpURLConnection.setRequestMethod(method);
        return httpURLConnection;
    }

    private URL appendGetParametersToUrlIfNecessary(URL url, Map<String, String> params, String method) throws MalformedURLException {
        //add URL parameters
        if(method.equals(HttpConstants.GET)&& params != null && !params.isEmpty()){
            url = new URL(url.toString()+"?" + mapToURLEncodedString(params));
            }

        return url;
    }

    private StringBuilder mapToURLEncodedString(Map<String, String> params) {
        val finalURL = new StringBuilder();
        if(!params.isEmpty()) {
            boolean firstParam = true;
            for (val key : params.keySet()) {
                if (!firstParam) {
                    finalURL.append("&");
                }
                firstParam = false;
                finalURL.append(key);
                finalURL.append("=");
                finalURL.append(params.get(key));
            }
        }
        return finalURL;
    }

    private static final byte[] NULL_BA = new byte[0];// can share these
    /**
     * Taken from jmeter.
     * Reads the response from the URL connection.
     *
     * @param conn
     *            URL from which to read response
     * @param res
     *            {@link RestResult} to read response into
     * @return response content
     * @exception IOException
     *                if an I/O exception occurs
     */
    private void readResponse(HttpURLConnection conn, RestResult res) throws IOException {
        BufferedInputStream in = null;

        final long contentLength = conn.getContentLength();
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
            if (! (e.getCause() instanceof FileNotFoundException))
            {
                log.error("readResponse: {}", e.toString());
                Throwable cause = e.getCause();
                if (cause != null){
                    log.error("Cause: {}", cause.toString());
                    if(cause instanceof Error) {
                        throw (Error)cause;
                    }
                }
            }
            // Normal InputStream is not available
            InputStream errorStream = conn.getErrorStream();
            if (errorStream == null) {
                if(log.isInfoEnabled()) {
                    log.info("Error Response Code: {}, Server sent no Errorpage", conn.getResponseCode());
                }
               res.setResponse(NULL_BA);
               res.setEndTime(System.nanoTime());
               res.setContentType(conn.getContentType());
               res.setHeaders(conn.getHeaderFields());
            }

            if(log.isInfoEnabled()) {
                log.info("Error Response Code: {}", conn.getResponseCode());
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
            log.error("readResponse: {}", e.toString());
            Throwable cause = e.getCause();
            if (cause != null){
                log.error("Cause: {}", cause.toString());
                if(cause instanceof Error) {
                    throw (Error)cause;
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
        res.setResponse(responseData);
        res.setEndTime(System.nanoTime());
        res.setContentType(conn.getContentType());
        res.setHeaders(conn.getHeaderFields());
    }
}
