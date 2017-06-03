package com.blade.mvc;

import com.blade.mvc.http.Request;
import com.blade.mvc.http.Response;
import com.blade.mvc.http.SessionManager;
import io.netty.util.concurrent.FastThreadLocal;

/**
 * Blade Web Context
 *
 * @author biezhi
 *         2017/6/1
 */
public class WebContext {

    // used netty fast theadLocal
    private static final FastThreadLocal<WebContext> fastThreadLocal = new FastThreadLocal<>();

    private Request request;
    private Response response;
    private SessionManager sessionManager;

    public WebContext(SessionManager sessionManager, Request request, Response response) {
        this.sessionManager = sessionManager;
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

    public static SessionManager sessionManager() {
        WebContext webContext = get();
        return null != webContext ? webContext.sessionManager : null;
    }
}
