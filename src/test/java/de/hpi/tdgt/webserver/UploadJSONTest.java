package de.hpi.tdgt.webserver;

import de.hpi.tdgt.RequestHandlingFramework;
import de.hpi.tdgt.Utils;
import de.hpi.tdgt.WebApplication;
import lombok.extern.log4j.Log4j2;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;
import java.net.URL;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = WebApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Log4j2
public class UploadJSONTest extends RequestHandlingFramework {
    @LocalServerPort
    private int port;
    private String exampleStory;

    @BeforeEach
    public void prepare() throws IOException {
        exampleStory = new Utils().getRequestExampleJSON();
    }
    @Autowired
    private TestRestTemplate restTemplate;
    @Test
    public void runsUserStoryAgainstTestServerReturns200() throws Exception {
        RequestEntity<String> requestEntity = RequestEntity .post(new URL("http://localhost:"+port+"/upload/"+System.currentTimeMillis()).toURI()) .contentType(MediaType.APPLICATION_JSON) .body(exampleStory);
        val response = restTemplate.exchange(requestEntity, String.class);
        assertThat(response.getStatusCode(), equalTo(HttpStatus.OK));
    }
    @Test
    public void runsUserStoryAgainstTestServerRunsActualTest() throws Exception {
        RequestEntity<String> requestEntity = RequestEntity .post(new URL("http://localhost:"+port+"/upload/"+System.currentTimeMillis()).toURI()) .contentType(MediaType.APPLICATION_JSON) .body(exampleStory);
        restTemplate.exchange(requestEntity, String.class);
        //requests to this handler are sent
        assertThat(authHandler.getNumberFailedLogins(), greaterThan(0));
    }

    @Test
    public void runsUserStoryAgainstTestServerRunsActualTestAlsoInCliMode() throws Exception {
        val args = new String[]{"cli","load", "./src/test/resources/de/hpi/tdgt/RequestExample.json", "./src/test/resources/de/hpi/tdgt", "./src/test/resources/de/hpi/tdgt"};
        WebApplication.main(args);
        //requests to this handler are sent
        assertThat(authHandler.getNumberFailedLogins(), greaterThan(0));
    }
    @Test
    public void runsUserStoryAgainstTestServerReturns415OnWrongContentType() throws Exception {
        RequestEntity<String> requestEntity = RequestEntity .post(new URL("http://localhost:"+port+"/upload/"+System.currentTimeMillis()).toURI()) .contentType(MediaType.APPLICATION_PDF) .body(exampleStory);
        val response = restTemplate.exchange(requestEntity, String.class);
        assertThat(response.getStatusCode(), equalTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE));
    }
    @Test
    public void runsUserStoryAgainstTestServerReturns400OnNotJSON() throws Exception {
        RequestEntity<String> requestEntity = RequestEntity .post(new URL("http://localhost:"+port+"/upload/"+System.currentTimeMillis()).toURI()) .contentType(MediaType.APPLICATION_JSON) .body("{");
        val response = restTemplate.exchange(requestEntity, String.class);
        assertThat(response.getStatusCode(), equalTo(HttpStatus.BAD_REQUEST));
    }
    @Test
    public void runsUserStoryAgainstTestServerReturns400OnNoContent() throws Exception {
        RequestEntity<String> requestEntity = RequestEntity .post(new URL("http://localhost:"+port+"/upload/"+System.currentTimeMillis()).toURI()) .contentType(MediaType.APPLICATION_JSON) .body("");
        val response = restTemplate.exchange(requestEntity, String.class);
        assertThat(response.getStatusCode(), equalTo(HttpStatus.BAD_REQUEST));
    }
}
