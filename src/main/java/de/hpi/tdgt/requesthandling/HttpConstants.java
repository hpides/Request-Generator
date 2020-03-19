package de.hpi.tdgt.requesthandling;

/**
 * Some HTTP constants for reuse.
 */
public class HttpConstants {
    //copied from jmeter
    public static final String SC_MOVED_PERMANENTLY = "301";
    public static final String SC_MOVED_TEMPORARILY = "302";
    public static final String SC_SEE_OTHER = "303";
    public static final String SC_TEMPORARY_REDIRECT = "307";

    public static final int DEFAULT_HTTPS_PORT = 443;
    public static final String DEFAULT_HTTPS_PORT_STRING = "443"; // $NON-NLS-1$
    public static final int DEFAULT_HTTP_PORT = 80;
    public static final String DEFAULT_HTTP_PORT_STRING = "80"; // $NON-NLS-1$
    public static final String PROTOCOL_HTTP = "http"; // $NON-NLS-1$
    public static final String PROTOCOL_HTTPS = "https"; // $NON-NLS-1$
    public static final String HEAD = "HEAD"; // $NON-NLS-1$
    public static final String POST = "POST"; // $NON-NLS-1$
    public static final String PUT = "PUT"; // $NON-NLS-1$
    public static final String GET = "GET"; // $NON-NLS-1$
    public static final String OPTIONS = "OPTIONS"; // $NON-NLS-1$
    public static final String TRACE = "TRACE"; // $NON-NLS-1$
    public static final String DELETE = "DELETE"; // $NON-NLS-1$
    public static final String PATCH = "PATCH"; // $NON-NLS-1$
    public static final String PROPFIND = "PROPFIND"; // $NON-NLS-1$
    public static final String PROPPATCH = "PROPPATCH"; // $NON-NLS-1$
    public static final String MKCOL = "MKCOL"; // $NON-NLS-1$
    public static final String COPY = "COPY"; // $NON-NLS-1$
    public static final  String MOVE = "MOVE"; // $NON-NLS-1$
    public static final String LOCK = "LOCK"; // $NON-NLS-1$
    public static final String UNLOCK = "UNLOCK"; // $NON-NLS-1$
    public static final String CONNECT = "CONNECT"; // $NON-NLS-1$
    public static final String REPORT = "REPORT"; // $NON-NLS-1$
    public static final String MKCALENDAR = "MKCALENDAR"; // $NON-NLS-1$
    public static final String SEARCH = "SEARCH"; // $NON-NLS-1$
    public static final String HEADER_AUTHORIZATION = "Authorization"; // $NON-NLS-1$
    public static final String HEADER_COOKIE = "Cookie"; // $NON-NLS-1$
    public static final String HEADER_COOKIE_IN_REQUEST = "Cookie:"; // $NON-NLS-1$
    public static final String HEADER_CONNECTION = "Connection"; // $NON-NLS-1$
    public static final String CONNECTION_CLOSE = "close"; // $NON-NLS-1$
    public static final String KEEP_ALIVE = "keep-alive"; // $NON-NLS-1$
    // e.g. "Transfer-Encoding: chunked", which is processed automatically by the underlying protocol
    public static final String TRANSFER_ENCODING = "transfer-encoding"; // $NON-NLS-1$
    public static final String HEADER_CONTENT_ENCODING = "content-encoding"; // $NON-NLS-1$
    public static final String HTTP_1_1 = "HTTP/1.1"; // $NON-NLS-1$
    public static final String HEADER_SET_COOKIE = "Set-Cookie"; // $NON-NLS-1$
    // Brotli compression not supported yet by HC4 4.5.2 , but to be added
    public static final String ENCODING_BROTLI = "br"; // $NON-NLS-1$
    public static final String ENCODING_DEFLATE = "deflate"; // $NON-NLS-1$
    public static final String ENCODING_GZIP = "gzip"; // $NON-NLS-1$

    public static final String HEADER_CONTENT_DISPOSITION = "Content-Disposition"; // $NON-NLS-1$
    public static final String HEADER_CONTENT_TYPE = "Content-Type"; // $NON-NLS-1$
    public static final String HEADER_CONTENT_LENGTH = "Content-Length"; // $NON-NLS-1$
    public static final String HEADER_HOST = "Host"; // $NON-NLS-1$
    public static final String HEADER_LOCAL_ADDRESS = "X-LocalAddress"; // $NON-NLS-1$ pseudo-header for reporting Local Address
    public static final String HEADER_LOCATION = "Location"; // $NON-NLS-1$
    public static final String APPLICATION_X_WWW_FORM_URLENCODED = "application/x-www-form-urlencoded"; // $NON-NLS-1$
    public static final String MULTIPART_FORM_DATA = "multipart/form-data"; // $NON-NLS-1$
    // For handling caching
    public static final String IF_NONE_MATCH = "If-None-Match"; // $NON-NLS-1$
    public static final String IF_MODIFIED_SINCE = "If-Modified-Since"; // $NON-NLS-1$
    public static final String ETAG = "Etag"; // $NON-NLS-1$
    public static final String LAST_MODIFIED = "Last-Modified"; // $NON-NLS-1$
    public static final String EXPIRES = "Expires"; // $NON-NLS-1$
    public static final String CACHE_CONTROL = "Cache-Control";  //e.g. public, max-age=259200
    public static final String DATE = "Date";  //e.g. Date Header of response
    public static final String VARY = "Vary"; // $NON-NLS-1$
    public static final String CONTENT_TYPE_TEXT_PLAIN = "text/plain";
    public static final String CONTENT_TYPE_APPLICATION_JSON = "application/json";
    public static final String CONTENT_TYPE_TEXT_PLAIN_UTF8 = CONTENT_TYPE_TEXT_PLAIN+ ";charset=utf-8";
}
