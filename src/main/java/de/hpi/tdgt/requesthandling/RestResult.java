package de.hpi.tdgt.requesthandling;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.squareup.okhttp.Headers;
import lombok.*;
import org.apache.http.Header;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class RestResult {
    //parameters for httpurlconnection
    private long startTime;
    private long endTime;
    @Setter(AccessLevel.PACKAGE)
    private byte[] response;
    private String contentType;
    private Headers headers;
    private int returnCode;

    //check content encoding
    public boolean isPlainText(){
        return contentType != null && contentType.replaceAll("\\s+","").toLowerCase().startsWith(HttpConstants.CONTENT_TYPE_TEXT_PLAIN);
    }
    public boolean isJSON(){
        //correct json content type requires a space ("application/json; charset=...")
        return  contentType != null &&contentType.toLowerCase().startsWith(HttpConstants.CONTENT_TYPE_APPLICATION_JSON);
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
        val contentTypeHeader = contentType.toLowerCase().split(";\\s?charset=");
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
}
