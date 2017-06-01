package com.blade.mvc;

import com.blade.http.Request;
import com.blade.http.Response;

/**
 * @author biezhi
 *         2017/5/31
 */
@FunctionalInterface
public interface RouteHandler {

    void handle(Request request, Response response);

}
