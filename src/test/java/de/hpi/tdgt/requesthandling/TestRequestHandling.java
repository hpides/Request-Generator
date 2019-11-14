package de.hpi.tdgt.requesthandling;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import lombok.val;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TestRequestHandling {
    //Web server setup taken from https://www.codeproject.com/tips/1040097/create-a-simple-web-server-in-java-http-server
    public static void parseQuery(String query, Map<String,
            Object> parameters) throws UnsupportedEncodingException {

        if (query != null) {
            String pairs[] = query.split("[&]");
            for (String pair : pairs) {
                String param[] = pair.split("[=]");
                String key = null;
                String value = null;
                if (param.length > 0) {
                    key = URLDecoder.decode(param[0],
                            System.getProperty("file.encoding"));
                }

                if (param.length > 1) {
                    value = URLDecoder.decode(param[1],
                            System.getProperty("file.encoding"));
                }

                if (parameters.containsKey(key)) {
                    Object obj = parameters.get(key);
                    if (obj instanceof List<?>) {
                        List<String> values = (List<String>) obj;
                        values.add(value);

                    } else if (obj instanceof String) {
                        List<String> values = new ArrayList<String>();
                        values.add((String) obj);
                        values.add(value);
                        parameters.put(key, values);
                    }
                } else {
                    parameters.put(key, value);
                }
            }
        }
    }
    private class GetHandler implements HttpHandler{

        @Override
        public void handle(HttpExchange he) throws IOException {
            // parse request
            Map<String, Object> parameters = new HashMap<String, Object>();
            URI requestedUri = he.getRequestURI();
            String query = requestedUri.getRawQuery();
            parseQuery(query, parameters);

            // send response
            StringBuilder responseBuilder = new StringBuilder();
            responseBuilder.append("Welcome!\n");
            for (String key : parameters.keySet())
                responseBuilder.append(key).append(" = ").append(parameters.get(key)).append("\n");
            String response = responseBuilder.toString();
            val headers = he.getResponseHeaders();
            headers.put(HttpConstants.HEADER_CONTENT_TYPE,Collections.singletonList(HttpConstants.CONTENT_TYPE_TEXT_PLAIN_UTF8));
            he.sendResponseHeaders(200, response.length());
            OutputStream os = he.getResponseBody();
            os.write(response.toString().getBytes());

            os.close();
        }
    }

    private class JSONObjectGetHandler implements HttpHandler{

        @Override
        public void handle(HttpExchange he) throws IOException {
            // parse request
            Map<String, Object> parameters = new HashMap<String, Object>();
            URI requestedUri = he.getRequestURI();
            String query = requestedUri.getRawQuery();
            parseQuery(query, parameters);

            // send response
            StringBuilder responseBuilder = new StringBuilder();
            responseBuilder.append("{\n");
            boolean first = true;
            for (String key : parameters.keySet()) {
                if (!first) {
                    responseBuilder.append(",");
                }
                first = false;
                responseBuilder.append("\"").append(key).append("\"").append(" : ").append("\"").append(parameters.get(key)).append("\"").append("\n");
            }
            responseBuilder.append("}");
            String response = responseBuilder.toString();
            val headers = he.getResponseHeaders();
            headers.put(HttpConstants.HEADER_CONTENT_TYPE,Collections.singletonList(HttpConstants.CONTENT_TYPE_APPLICATION_JSON));
            he.sendResponseHeaders(200, response.length());
            OutputStream os = he.getResponseBody();
            os.write(response.toString().getBytes());

            os.close();
        }
    }
    private class JSONArrayGetHandler implements HttpHandler{

        @Override
        public void handle(HttpExchange he) throws IOException {
            // parse request
            Map<String, Object> parameters = new HashMap<String, Object>();
            URI requestedUri = he.getRequestURI();
            String query = requestedUri.getRawQuery();
            parseQuery(query, parameters);

            // send response
            StringBuilder responseBuilder = new StringBuilder();
            responseBuilder.append("[\n");
            boolean first = true;
            for (String key : parameters.keySet()) {
                if (!first) {
                    responseBuilder.append(",");
                }
                first = false;
                responseBuilder.append("{\"").append(key).append("\"").append(" : ").append("\"").append(parameters.get(key)).append("\"}").append("\n");
            }
            responseBuilder.append("]");
            String response = responseBuilder.toString();
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            val headers = he.getResponseHeaders();
            headers.put(HttpConstants.HEADER_CONTENT_TYPE,Collections.singletonList(HttpConstants.CONTENT_TYPE_APPLICATION_JSON));
            he.sendResponseHeaders(200, response.length());
            OutputStream os = he.getResponseBody();
            os.write(response.toString().getBytes());

            os.close();
        }
    }

    private class PostHandler implements HttpHandler {

        @Override

        public void handle(HttpExchange he) throws IOException {
            // parse request
            Map<String, Object> parameters = new HashMap<String, Object>();
            InputStreamReader isr = new InputStreamReader(he.getRequestBody(), "utf-8");
            BufferedReader br = new BufferedReader(isr);
            String query = br.readLine();
            parseQuery(query, parameters);

            // send response
            StringBuilder responseBuilder = new StringBuilder();
            for (String key : parameters.keySet())
                responseBuilder.append(key).append(" = ").append(parameters.get(key)).append("\n");
            String response = responseBuilder.toString();
            val headers = he.getResponseHeaders();
            headers.put(HttpConstants.HEADER_CONTENT_TYPE,Collections.singletonList(HttpConstants.CONTENT_TYPE_TEXT_PLAIN_UTF8));
            he.sendResponseHeaders(200, response.length());
            OutputStream os = he.getResponseBody();
            os.write(response.toString().getBytes());
            os.close();
        }
    }
    private class PostBodyHandler implements HttpHandler {

        @Override

        public void handle(HttpExchange he) throws IOException {
            // parse request
            Map<String, Object> parameters = new HashMap<String, Object>();
            InputStreamReader isr = new InputStreamReader(he.getRequestBody(), "utf-8");
            BufferedReader br = new BufferedReader(isr);
            StringBuilder body = new StringBuilder();
            String query = br.readLine();
            body.append(query);
            while((query = br.readLine()) != null){
                body.append(query);
            }
            // send response
            String response = body.toString();
            val headers = he.getResponseHeaders();
            headers.put(HttpConstants.HEADER_CONTENT_TYPE,Collections.singletonList(HttpConstants.CONTENT_TYPE_APPLICATION_JSON));
            he.sendResponseHeaders(200, response.length());
            OutputStream os = he.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }
    @BeforeAll
    public void launchTestServer() throws IOException {
        int port = 9000;
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        System.out.println("server started at " + port);
        server.createContext("/", new GetHandler());
        server.createContext("/jsonObject", new JSONObjectGetHandler());
        server.createContext("/jsonArray", new JSONArrayGetHandler());
        server.createContext("/echoPost", new PostHandler());
        server.createContext("/postWithBody", new PostBodyHandler());
        server.createContext("/putWithBody", new PostBodyHandler());
        server.setExecutor(null);
        server.start();
    }
    @Test
    public void testSimpleRequest() throws IOException {
        val rc = new RestClient();
        val result = rc.getFromEndpoint(new URL("http://localhost:9000/"), new HashMap<>());
        String response = new String(result.getResponse(),StandardCharsets.UTF_8);
        assertThat(response, equalTo("Welcome!\n"));
    }

    @Test
    public void testContentType() throws IOException {
        val rc = new RestClient();
        val result = rc.getFromEndpoint(new URL("http://localhost:9000/"), new HashMap<>());
        assertThat(result.isPlainText(), is(true));
    }
    @Test
    public void testContentDecoding() throws IOException {
        val rc = new RestClient();
        val result = rc.getFromEndpoint(new URL("http://localhost:9000/"), new HashMap<>());
        assertThat(result.toString(), equalTo("Welcome!\n"));
    }

    @Test
    public void testGETParams() throws IOException {
        val rc = new RestClient();
        val params = new HashMap<String,String>();
        params.put("param","value");
        val result = rc.getFromEndpoint(new URL("http://localhost:9000/"), params);
        assertThat(result.toString(), stringContainsInOrder("param","value"));
    }

    @Test
    public void testJSON() throws IOException {
        val rc = new RestClient();
        val params = new HashMap<String,String>();
        params.put("param","value");
        val result = rc.getFromEndpoint(new URL("http://localhost:9000/jsonObject"), params);
        assertThat(result.toJson().isObject(),is(true));
    }
    @Test
    public void testJSONArray() throws IOException {
        val rc = new RestClient();
        val params = new HashMap<String,String>();
        params.put("param","value");
        val result = rc.getFromEndpoint(new URL("http://localhost:9000/jsonArray"), params);
        assertThat(result.toJson().isArray(),is(true));
    }

    @Test
    public void testMeasuresTime() throws IOException {
        val rc = new RestClient();
        val params = new HashMap<String,String>();
        params.put("param","value");
        val result = rc.getFromEndpoint(new URL("http://localhost:9000/jsonArray"), params);
        assertThat(result.durationMillis(), is(both(greaterThan(0L)).and(lessThan(100000L))));
    }

    @Test
    public void testPOSTFormParams() throws IOException {
        val rc = new RestClient();
        val params = new HashMap<String,String>();
        params.put("param","value");
        val result = rc.postFormToEndpoint(new URL("http://localhost:9000/echoPost"), params);
        assertThat(result.toString(), stringContainsInOrder("param = value"));
    }
    @Test
    public void testPUTFormParams() throws IOException {
        val rc = new RestClient();
        val params = new HashMap<String,String>();
        params.put("param","value");
        val result = rc.putFormToEndpoint(new URL("http://localhost:9000/echoPost"), params);
        assertThat(result.toString(), stringContainsInOrder("param = value"));
    }

    @Test
    public void testPOSTBodyParams() throws IOException {
        val rc = new RestClient();
        val body = "{\"param\":\"value\"}";
        val result = rc.postBodyToEndpoint(new URL("http://localhost:9000/postWithBody"), body);
        assertThat(result.toString(), equalTo(body));
    }
    @Test
    public void testPUTBodyParams() throws IOException {
        val rc = new RestClient();
        val body = "{\"param\":\"value\"}";
        val result = rc.putBodyToEndpoint(new URL("http://localhost:9000/postWithBody"), body);
        assertThat(result.toString(), equalTo(body));
    }


}
