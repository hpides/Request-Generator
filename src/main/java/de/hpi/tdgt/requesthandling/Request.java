package de.hpi.tdgt.requesthandling;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.IOException;
import java.net.URL;
import java.util.Map;

/**
 * Represents a HTTP Request.
 */
@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
public class Request {
    URL url;
    /**
     * Params for GET URL / POST / PUT Form-Encoded data.
     */
    private Map<String, String> params=null;
    private String method;
    private boolean followsRedirects=true;
    private int connectTimeout=-1;
    private int responseTimeout=-1;
    private boolean sendKeepAlive=false;
    private int retries=0;
    /**
     * Body for POST/PUT.
     */
    private String body=null;
    /**
     * Set to true if using PUT / POST you want to send URL-Encoded parameters. Else set to false.
     */
    private boolean form=false;

    private String username = null, password = null;
}
