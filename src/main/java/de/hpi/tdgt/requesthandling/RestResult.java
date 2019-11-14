package de.hpi.tdgt.requesthandling;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.val;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
public class RestResult {
    private long startTime;
    private long endTime;
    private byte[] response;
    private String contentType;
    private Map<String, List<String>> headers;
    private int returnCode;
    public boolean isPlainText(){
        return contentType.replaceAll("\\s+","").toLowerCase().startsWith(HttpConstants.CONTENT_TYPE_TEXT_PLAIN);
    }
    public boolean isJSON(){
        return contentType.replaceAll("\\s+","").toLowerCase().startsWith(HttpConstants.CONTENT_TYPE_APPLICATION_JSON);
    }
    @Override
    public String toString(){
        if(isPlainText() | isJSON()){
                return new String(response, getCharset());
        }
        //can not return a string representation
        else {
            return null;
        }
    }

    private Charset getCharset() {
        val contentTypeHeader = contentType.toLowerCase().split(";charset=");
        if(contentTypeHeader.length == 2){
            return Charset.forName(contentTypeHeader[1]);
        }
        return StandardCharsets.US_ASCII;
    }
    private final ObjectMapper mapper = new ObjectMapper();
    public JsonNode toJson() throws IOException {
        if(!isJSON()){
            return null;
        }
        return mapper.readTree(response);
    }

    public long durationNanos(){
        return endTime - startTime;
    }

    public long durationMillis(){
        return durationNanos() / 1000000;
    }
}
