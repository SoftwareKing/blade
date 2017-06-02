package com.blade.mvc.http;

import com.blade.mvc.ui.ModelAndView;

/**
 * @author biezhi
 *         2017/5/31
 */
public interface Response {

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
     * Setting header
     *
     * @param name  Header Name
     * @param value Header Value
     * @return Return Response
     */
    Response header(String name, String value);

    Response cookie(Cookie cookie);

    /**
     * Setting Cookie
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

    boolean isCommit();

}
