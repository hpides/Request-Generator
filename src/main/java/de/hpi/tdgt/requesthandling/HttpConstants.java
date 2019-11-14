package de.hpi.tdgt.requesthandling;

public class HttpConstants {
    //copied from jmeter
    public static String SC_MOVED_PERMANENTLY = "301";
    public static String SC_MOVED_TEMPORARILY = "302";
    public static String SC_SEE_OTHER = "303";
    public static String SC_TEMPORARY_REDIRECT = "307";

    public static int DEFAULT_HTTPS_PORT = 443;
    public static String DEFAULT_HTTPS_PORT_STRING = "443"; // $NON-NLS-1$
    public static int DEFAULT_HTTP_PORT = 80;
    public static String DEFAULT_HTTP_PORT_STRING = "80"; // $NON-NLS-1$
    public static String PROTOCOL_HTTP = "http"; // $NON-NLS-1$
    public static String PROTOCOL_HTTPS = "https"; // $NON-NLS-1$
    public static String HEAD = "HEAD"; // $NON-NLS-1$
    public static String POST = "POST"; // $NON-NLS-1$
    public static String PUT = "PUT"; // $NON-NLS-1$
    public static String GET = "GET"; // $NON-NLS-1$
    public static String OPTIONS = "OPTIONS"; // $NON-NLS-1$
    public static String TRACE = "TRACE"; // $NON-NLS-1$
    public static String DELETE = "DELETE"; // $NON-NLS-1$
    public static String PATCH = "PATCH"; // $NON-NLS-1$
    public static String PROPFIND = "PROPFIND"; // $NON-NLS-1$
    public static String PROPPATCH = "PROPPATCH"; // $NON-NLS-1$
    public static String MKCOL = "MKCOL"; // $NON-NLS-1$
    public static String COPY = "COPY"; // $NON-NLS-1$
    public static  String MOVE = "MOVE"; // $NON-NLS-1$
    public static String LOCK = "LOCK"; // $NON-NLS-1$
    public static String UNLOCK = "UNLOCK"; // $NON-NLS-1$
    public static String CONNECT = "CONNECT"; // $NON-NLS-1$
    public static String REPORT = "REPORT"; // $NON-NLS-1$
    public static String MKCALENDAR = "MKCALENDAR"; // $NON-NLS-1$
    public static String SEARCH = "SEARCH"; // $NON-NLS-1$
    public static String HEADER_AUTHORIZATION = "Authorization"; // $NON-NLS-1$
    public static String HEADER_COOKIE = "Cookie"; // $NON-NLS-1$
    public static String HEADER_COOKIE_IN_REQUEST = "Cookie:"; // $NON-NLS-1$
    public static String HEADER_CONNECTION = "Connection"; // $NON-NLS-1$
    public static String CONNECTION_CLOSE = "close"; // $NON-NLS-1$
    public static String KEEP_ALIVE = "keep-alive"; // $NON-NLS-1$
    // e.g. "Transfer-Encoding: chunked", which is processed automatically by the underlying protocol
    public static String TRANSFER_ENCODING = "transfer-encoding"; // $NON-NLS-1$
    public static String HEADER_CONTENT_ENCODING = "content-encoding"; // $NON-NLS-1$
    public static String HTTP_1_1 = "HTTP/1.1"; // $NON-NLS-1$
    public static String HEADER_SET_COOKIE = "set-cookie"; // $NON-NLS-1$
    // Brotli compression not supported yet by HC4 4.5.2 , but to be added
    public static String ENCODING_BROTLI = "br"; // $NON-NLS-1$
    public static String ENCODING_DEFLATE = "deflate"; // $NON-NLS-1$
    public static String ENCODING_GZIP = "gzip"; // $NON-NLS-1$

    public static String HEADER_CONTENT_DISPOSITION = "Content-Disposition"; // $NON-NLS-1$
    public static String HEADER_CONTENT_TYPE = "Content-Type"; // $NON-NLS-1$
    public static String HEADER_CONTENT_LENGTH = "Content-Length"; // $NON-NLS-1$
    public static String HEADER_HOST = "Host"; // $NON-NLS-1$
    public static String HEADER_LOCAL_ADDRESS = "X-LocalAddress"; // $NON-NLS-1$ pseudo-header for reporting Local Address
    public static String HEADER_LOCATION = "Location"; // $NON-NLS-1$
    public static String APPLICATION_X_WWW_FORM_URLENCODED = "application/x-www-form-urlencoded"; // $NON-NLS-1$
    public static String MULTIPART_FORM_DATA = "multipart/form-data"; // $NON-NLS-1$
    // For handling caching
    public static String IF_NONE_MATCH = "If-None-Match"; // $NON-NLS-1$
    public static String IF_MODIFIED_SINCE = "If-Modified-Since"; // $NON-NLS-1$
    public static String ETAG = "Etag"; // $NON-NLS-1$
    public static String LAST_MODIFIED = "Last-Modified"; // $NON-NLS-1$
    public static String EXPIRES = "Expires"; // $NON-NLS-1$
    public static String CACHE_CONTROL = "Cache-Control";  //e.g. public, max-age=259200
    public static String DATE = "Date";  //e.g. Date Header of response
    public static String VARY = "Vary"; // $NON-NLS-1$
    public static String CONTENT_TYPE_TEXT_PLAIN = "text/plain";
    public static String CONTENT_TYPE_APPLICATION_JSON = "application/json";
    public static String CONTENT_TYPE_TEXT_PLAIN_UTF8 = CONTENT_TYPE_TEXT_PLAIN+ ";charset=utf-8";
}
