package com.blade.mvc.interceptor;

import com.blade.mvc.http.Request;
import com.blade.mvc.http.Response;

/**
 * Interceptor, In the routing block before and after the execution.
 *
 * @author <a href="mailto:biezhi.me@gmail.com" target="_blank">biezhi</a>
 * @since 1.5
 */
public interface Interceptor {

    boolean before(Request request, Response response);

    boolean after(Request request, Response response);

}