package com.blade.mvc.http;

import com.blade.mvc.multipart.FileItem;
import io.netty.buffer.ByteBuf;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Http Request
 *
 * @author biezhi
 *         2017/5/31
 */
public interface Request {

    /**
     * init request path parameters
     *
     * @param pathParams
     * @return
     */
    Request initPathParams(Map<String, String> pathParams);

    /**
     * @return Return client request host
     */
    String host();

    /**
     * @return Return request uri
     */
    String uri();

    /**
     * @return request url
     */
    String url();

    String userAgent();

    /**
     * @return Return protocol
     */
    String protocol();

    /**
     * @return Return contextPath
     */
    String contextPath();

    /**
     * @return Return parameters on the path Map
     */
    Map<String, String> pathParams();

    /**
     * Get a URL parameter
     *
     * @param name Parameter name
     * @return Return parameter value
     */
    String pathString(String name);

    /**
     * Return a URL parameter for a Int type
     *
     * @param name Parameter name
     * @return Return Int parameter value
     */
    Integer pathInt(String name);

    /**
     * Return a URL parameter for a Long type
     *
     * @param name Parameter name
     * @return Return Long parameter value
     */
    Long pathLong(String name);

    /**
     * @return Return query string
     */
    String queryString();

    /**
     * @return Return request query Map
     */
    Map<String, List<String>> querys();

    /**
     * Get a request parameter
     *
     * @param name Parameter name
     * @return Return request parameter value
     */
    Optional<String> query(String name);

    /**
     * Get a request parameter, if NULL is returned to defaultValue
     *
     * @param name         parameter name
     * @param defaultValue default String value
     * @return Return request parameter values
     */
    String query(String name, String defaultValue);

    /**
     * Returns a request parameter for a Int type
     *
     * @param name Parameter name
     * @return Return Int parameter values
     */
    Optional<Integer> queryInt(String name);

    /**
     * Returns a request parameter for a Int type
     *
     * @param name         Parameter name
     * @param defaultValue default int value
     * @return Return Int parameter values
     */
    int queryInt(String name, int defaultValue);

    /**
     * Returns a request parameter for a Long type
     *
     * @param name Parameter name
     * @return Return Long parameter values
     */
    Optional<Long> queryLong(String name);

    /**
     * Returns a request parameter for a Long type
     *
     * @param name         Parameter name
     * @param defaultValue default long value
     * @return Return Long parameter values
     */
    long queryLong(String name, long defaultValue);

    /**
     * Returns a request parameter for a Double type
     *
     * @param name Parameter name
     * @return Return Double parameter values
     */
    Optional<Double> queryDouble(String name);

    /**
     * Returns a request parameter for a Double type
     *
     * @param name         Parameter name
     * @param defaultValue default double value
     * @return Return Double parameter values
     */
    double queryDouble(String name, double defaultValue);

    /**
     * @return Return request method
     */
    String method();

    /**
     * @return Return HttpMethod
     */
    HttpMethod httpMethod();

    /**
     * @return Return server remote address
     */
    String address();

    /**
     * @return Return current session
     */
    Session session();

    /**
     * @return Return contentType
     */
    String contentType();

    /**
     * @return Return whether to use the SSL connection
     */
    boolean isSecure();

    /**
     * @return Return current request is a AJAX request
     */
    boolean isAjax();

    boolean isIE();

    Map<String, String> cookies();

    /**
     * Get String Cookie Value
     *
     * @param name cookie name
     * @return Return Cookie Value
     */
    Optional<String> cookie(String name);

    Optional<Cookie> cookieRaw(String name);

    /**
     * Get String Cookie Value
     *
     * @param name         cookie name
     * @param defaultValue default cookie value
     * @return Return Cookie Value
     */
    String cookie(String name, String defaultValue);

    /**
     * Add a cookie to the request
     *
     * @param cookie
     * @return
     */
    Request cookie(Cookie cookie);

    /**
     * @return Return header information Map
     */
    Map<String, String> headers();

    /**
     * Get header information
     *
     * @param name Parameter name
     * @return Return header information
     */
    String header(String name);

    /**
     * Get header information
     *
     * @param name         Parameter name
     * @param defaultValue default header value
     * @return Return header information
     */
    String header(String name, String defaultValue);

    /**
     * @return return current request connection keepAlive
     */
    boolean keepAlive();

    /**
     * Setting Request Attribute
     *
     * @param name  Parameter name
     * @param value Parameter Value
     */
    Request attribute(String name, Object value);

    /**
     * Get a Request Attribute
     *
     * @param name Parameter name
     * @return Return parameter value
     */
    <T> T attribute(String name);

    /**
     * @return Return all Attribute in Request
     */
    Map<String, Object> attributes();

    /**
     * @return return request file items
     */
    Map<String, FileItem> fileItems();

    /**
     * get file item by request part name
     *
     * @param name
     * @return
     */
    Optional<FileItem> fileItem(String name);

    /**
     * @return Return request body
     */
    ByteBuf body();

    /**
     * @return return request body to string
     */
    String bodyToString();

}
