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
    private String[] requestParams;
    private String[] responseParams;

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
