package de.hpi.tdgt.requesthandling;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.val;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

public class RestResult {
    //parameters for httpurlconnection
    private long startTime;
    private long endTime;
    private byte[] response;
    private String contentType;
    private Map<String, List<String>> headers;
    private int returnCode;
    private Exception errorCondition = null;
    //check content encoding
    public boolean isPlainText(){
        return contentType != null && contentType.replaceAll("\\s+","").toLowerCase().startsWith(HttpConstants.CONTENT_TYPE_TEXT_PLAIN);
    }
    public boolean isJSON(){
        return  contentType != null && contentType.replaceAll("\\s+","").toLowerCase().startsWith(HttpConstants.CONTENT_TYPE_APPLICATION_JSON);
    }
    //use directly or deserialize
    @Override
    public String toString(){
        if(isPlainText() || isJSON()){
                return new String(response, getCharset());
        }
        //can not return a string representation
        else {
            return "";
        }
    }
    //parse contenttype header
    private Charset getCharset() {
        val contentTypeHeader = contentType.toLowerCase().split(";charset=");
        if(contentTypeHeader.length == 2){
            return Charset.forName(contentTypeHeader[1]);
        }
        return StandardCharsets.US_ASCII;
    }
    private final ObjectMapper mapper = new ObjectMapper();
    //return JsonNode, because we do not know if it is array or object
    public JsonNode toJson() throws IOException {
        if(!isJSON()){
            return null;
        }
        return mapper.readTree(response);
    }
    //calculate durations
    public long durationNanos(){
        return endTime - startTime;
    }

    public long durationMillis(){
        return durationNanos() / 1000000;
    }

    public long getStartTime() {
        return this.startTime;
    }

    public long getEndTime() {
        return this.endTime;
    }

    public byte[] getResponse() {
        return this.response;
    }

    public String getContentType() {
        return this.contentType;
    }

    public Map<String, List<String>> getHeaders() {
        return this.headers;
    }

    public int getReturnCode() {
        return this.returnCode;
    }

    public Exception getErrorCondition() {
        return this.errorCondition;
    }

    public ObjectMapper getMapper() {
        return this.mapper;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public void setHeaders(Map<String, List<String>> headers) {
        this.headers = headers;
    }

    public void setReturnCode(int returnCode) {
        this.returnCode = returnCode;
    }

    public void setErrorCondition(Exception errorCondition) {
        this.errorCondition = errorCondition;
    }

    void setResponse(byte[] response) {
        this.response = response;
    }
}
