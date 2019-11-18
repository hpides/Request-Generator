package de.hpi.tdgt.requesthandling;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import lombok.Getter;
import lombok.val;

import java.io.*;
import java.net.URI;
import java.net.URLDecoder;
import java.util.*;
import java.util.stream.Collectors;

public class HttpHandlers {
    //Classes and methods based on https://www.codeproject.com/tips/1040097/create-a-simple-web-server-in-java-http-server

    /**
     * Reads an urlencodet query to a map.
     * @param query query
     * @param parameters target
     * @throws UnsupportedEncodingException
     */
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
                        List<String> values = ((List<?>) obj).parallelStream().map(Object::toString).collect(Collectors.toList());
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
    public static class GetHandler implements HttpHandler {
        @Getter
        private Map<String,Object> lastParameters = null;
        @Override
        public void handle(HttpExchange he) throws IOException {
            // parse request
            Map<String, Object> parameters = new HashMap<String, Object>();
            URI requestedUri = he.getRequestURI();
            String query = requestedUri.getRawQuery();
            parseQuery(query, parameters);
            lastParameters = parameters;
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

    /**
     * Makes request URL Parameters to a JSON Object with the Request keys as keys and their values as values.
     */
    public static class JSONObjectGetHandler implements HttpHandler{
        @Getter
        private Map<String, Object> lastParameters = null;
        @Override
        public void handle(HttpExchange he) throws IOException {
            // parse request
            Map<String, Object> parameters = new HashMap<String, Object>();
            URI requestedUri = he.getRequestURI();
            String query = requestedUri.getRawQuery();
            parseQuery(query, parameters);
            lastParameters = parameters;
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
    /**
     * Makes request URL Parameters to a JSON Array of Objects with the Request keys as key for an object and their values as values.
     */
    public static class JSONArrayGetHandler implements HttpHandler{

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

    /**
     * Expects post.
     */
    public static class PostHandler implements HttpHandler {

        @Override

        public void handle(HttpExchange he) throws IOException {
            // parse request
            Map<String, Object> parameters = new HashMap<String, Object>();
            InputStreamReader isr = new InputStreamReader(he.getRequestBody(), "utf-8");
            var headers = he.getRequestHeaders();
            val contentType = headers.getFirst(HttpConstants.HEADER_CONTENT_TYPE);
            if(contentType == null || !contentType.equals(HttpConstants.APPLICATION_X_WWW_FORM_URLENCODED)){
                val message = "Missing Content Type Header!";
                headers = he.getResponseHeaders();
                headers.put(HttpConstants.HEADER_CONTENT_TYPE,Collections.singletonList(HttpConstants.CONTENT_TYPE_APPLICATION_JSON));
                he.sendResponseHeaders(200, message.length());
                OutputStream os = he.getResponseBody();
                os.write(message.getBytes());
                os.close();
                return;
            }
            BufferedReader br = new BufferedReader(isr);
            String query = br.readLine();
            parseQuery(query, parameters);

            // send response
            StringBuilder responseBuilder = new StringBuilder();
            for (String key : parameters.keySet())
                responseBuilder.append(key).append(" = ").append(parameters.get(key)).append("\n");
            String response = responseBuilder.toString();
            headers = he.getResponseHeaders();
            headers.put(HttpConstants.HEADER_CONTENT_TYPE,Collections.singletonList(HttpConstants.CONTENT_TYPE_TEXT_PLAIN_UTF8));
            he.sendResponseHeaders(200, response.length());
            OutputStream os = he.getResponseBody();
            os.write(response.toString().getBytes());
            os.close();
        }
    }

    /**
     * Expects request in body.
     */
    public static class PostBodyHandler implements HttpHandler {
        @Getter
        private Map<String, Object> lastParameters = null;

        @Override
        public void handle(HttpExchange he) throws IOException {
            // parse request
            lastParameters = new HashMap<String, Object>();
            var headers = he.getRequestHeaders();
            val contentType = headers.getFirst(HttpConstants.HEADER_CONTENT_TYPE);
            if(contentType == null || !contentType.equals(HttpConstants.CONTENT_TYPE_APPLICATION_JSON)){
                val message = "Missing Content Type Header!";
                headers = he.getResponseHeaders();
                headers.put(HttpConstants.HEADER_CONTENT_TYPE,Collections.singletonList(HttpConstants.CONTENT_TYPE_APPLICATION_JSON));
                he.sendResponseHeaders(200, message.length());
                OutputStream os = he.getResponseBody();
                os.write(message.getBytes());
                os.close();
                return;
            }
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
            headers = he.getResponseHeaders();
            headers.put(HttpConstants.HEADER_CONTENT_TYPE,Collections.singletonList(HttpConstants.CONTENT_TYPE_APPLICATION_JSON));
            he.sendResponseHeaders(200, response.length());
            OutputStream os = he.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }

    /**
     * Expects requests to be authorized with username (hardcoded "user") and password (hardcoded "password").
     * Saves in the field "lastLoginWasOK" if the last login used the correct username and password.
     */
    public static class AuthHandler implements HttpHandler {
        static final String username = "user";
        static final String password = "pw";
        @Getter
        private boolean lastLoginWasOK = false;
        @Override
        public void handle(HttpExchange he) throws IOException {
            lastLoginWasOK = false;
            val requestHeaders = he.getRequestHeaders();
            var auth = requestHeaders.getFirst(HttpConstants.HEADER_AUTHORIZATION);
            if(auth != null && auth.startsWith("Basic ")){
                auth = auth.substring(auth.indexOf("Basic ")+"Basic ".length());
            }
            if(auth != null && Base64.getDecoder().decode(auth) != null && new String(Base64.getDecoder().decode(auth)).equals(username+":"+password)) {
                lastLoginWasOK = false;
                String response = "OK";
                val headers = he.getResponseHeaders();
                headers.put(HttpConstants.HEADER_CONTENT_TYPE, Collections.singletonList(HttpConstants.CONTENT_TYPE_TEXT_PLAIN));
                he.sendResponseHeaders(200, response.length());
                OutputStream os = he.getResponseBody();
                os.write(response.getBytes());
                os.close();
            }
            else {
                lastLoginWasOK = true;
                String response = "UNAUTHORIZED";
                val headers = he.getResponseHeaders();
                headers.put(HttpConstants.HEADER_CONTENT_TYPE, Collections.singletonList(HttpConstants.CONTENT_TYPE_TEXT_PLAIN));
                he.sendResponseHeaders(401, response.length());
                OutputStream os = he.getResponseBody();
                os.write(response.getBytes());
                os.close();
            }
        }
    }
}
