package com.blade.mvc.http;

import com.blade.mvc.ui.ModelAndView;
import com.blade.mvc.ui.RestResponse;
import io.netty.buffer.ByteBuf;

import java.io.File;
import java.nio.Buffer;
import java.util.Map;

/**
 * Http Response
 *
 * @author biezhi
 *         2017/5/31
 */
public interface Response {

    /**
     * @return return response status code
     */
    int statusCode();

    /**
     * Setting Response Status
     *
     * @param status status code
     * @return Return Response
     */
    Response status(int status);

    /**
     * @return Setting Response Status is BadRequest and Return Response
     */
    Response badRequest();

    /**
     * @return Setting Response Status is unauthorized and Return Response
     */
    Response unauthorized();

    /**
     * @return Setting Response Status is notFound and Return Response
     */
    Response notFound();

    /**
     * Setting Response ContentType
     *
     * @param contentType content type
     * @return Return Response
     */
    Response contentType(String contentType);

    /**
     * @return return response content-type
     */
    String contentType();

    /**
     * @return return response headers
     */
    Map<String, String> headers();

    /**
     * setting header
     *
     * @param name  Header Name
     * @param value Header Value
     * @return Return Response
     */
    Response header(String name, String value);

    /**
     * add raw response cookie
     *
     * @param cookie
     * @return
     */
    Response cookie(Cookie cookie);

    /**
     * add Cookie
     *
     * @param name  Cookie Name
     * @param value Cookie Value
     * @return Return Response
     */
    Response cookie(String name, String value);

    /**
     * Setting Cookie
     *
     * @param name   Cookie Name
     * @param value  Cookie Value
     * @param maxAge Period of validity
     * @return Return Response
     */
    Response cookie(String name, String value, int maxAge);

    /**
     * Setting Cookie
     *
     * @param name    Cookie Name
     * @param value   Cookie Value
     * @param maxAge  Period of validity
     * @param secured Is SSL
     * @return Return Response
     */
    Response cookie(String name, String value, int maxAge, boolean secured);

    /**
     * Setting Cookie
     *
     * @param path    Cookie Domain Path
     * @param name    Cookie Name
     * @param value   Cookie Value
     * @param maxAge  Period of validity
     * @param secured Is SSL
     * @return Return Response
     */
    Response cookie(String path, String name, String value, int maxAge, boolean secured);

    /**
     * remove cookie
     *
     * @param name
     * @return
     */
    Response removeCookie(String name);

    /**
     * @return return response cookies
     */
    Map<String, String> cookies();

    /**
     * Render by text
     *
     * @param text text content
     * @return Return Response
     */
    void text(String text);

    /**
     * Render by html
     *
     * @param html html content
     * @return Return Response
     */
    void html(String html);

    /**
     * Render by json
     *
     * @param json json content
     * @return Return Response
     */
    void json(String json);

    /**
     * Render by json
     *
     * @param bean
     * @return
     */
    void json(Object bean);

    /**
     * send body to client
     *
     * @param data
     */
    void body(String data);

    void body(byte[] data);

    void body(ByteBuf byteBuf);

    /**
     * download some file to clinet
     *
     * @param fileName give client file name
     * @param file
     */
    void donwload(String fileName, File file) throws Exception;

    /**
     * Render view
     *
     * @param view view page
     * @return Return Response
     */
    void render(String view);

    /**
     * Render view And Setting Data
     *
     * @param modelAndView ModelAndView object
     * @return Return Response
     */
    void render(ModelAndView modelAndView);

    /**
     * Redirect to newUri
     *
     * @param newUri new url
     */
    void redirect(String newUri);

    /**
     * @return return current response is commit
     */
    boolean isCommit();

}
