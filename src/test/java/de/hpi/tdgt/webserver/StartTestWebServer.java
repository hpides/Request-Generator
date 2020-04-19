package de.hpi.tdgt.webserver;

import de.hpi.tdgt.RequestHandlingFramework;

import java.io.IOException;

public class StartTestWebServer {
    public static void main(String[] args) throws IOException {
        RequestHandlingFramework f = new RequestHandlingFramework();
        f.launchTestServer();
    }
}
