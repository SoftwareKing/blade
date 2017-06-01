package com.blade.mvc;

import com.blade.http.Request;
import com.blade.http.Response;
import io.netty.util.concurrent.FastThreadLocal;

/**
 * @author biezhi
 *         2017/6/1
 */
public class WebContext {

    private static final FastThreadLocal<WebContext> fastThreadLocal = new FastThreadLocal<>();

    private Request request;
    private Response response;

    public WebContext(Request request, Response response) {
        this.request = request;
        this.response = response;
    }

    public static void set(WebContext webContext) {
        fastThreadLocal.set(webContext);
    }

    public static WebContext get() {
        return fastThreadLocal.get();
    }

    public static void remove() {
        fastThreadLocal.remove();
    }

    public static Request request() {
        WebContext webContext = get();
        return null != webContext ? webContext.request : null;
    }

    public static Response response() {
        WebContext webContext = get();
        return null != webContext ? webContext.response : null;
    }

}
