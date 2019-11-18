package de.hpi.tdgt.test.story.activity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class Request extends Activity{
    private String verb;
    private String addr;
    /**
     * Expected usage: values of this arrays are keys.
     * Use them as keys in a HTTP Form in a Request Body, get values for these keys from passed dict.
     */
    private String[] requestParams;
    /**
     * Expected usage: values of this arrays are keys.
     * Get the values for these keys from a response body Form and store them in the dict passed to the successors.
     */
    private String[] responseParams;
    /**
     * Expected usage: values of this arrays are keys.
     * Use them as keys in a JSON Object in a Request Body, get values for these keys from passed dict.
     */
    private String[] requestJSONObject;
    /**
     * Expected usage: values of this arrays are keys.
     * Get the values for these keys from a response body JSON object and store them in the dict passed to the successors.
     */
    private String[] responseJSONObject;

    private BasicAuth basicAuth;

    @Override
    public void perform() {
        //TODO implement

    }
    @Getter
    @Setter
    @NoArgsConstructor
    private class BasicAuth{
        private String user;
        private String password;
    }
}
