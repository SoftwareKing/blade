package com.blade.mvc.route;

import com.blade.mvc.hook.Invoker;
import com.blade.mvc.http.Request;
import com.blade.mvc.http.Response;
import com.blade.mvc.middleware.Middleware;
import com.blade.mvc.middleware.MiddlewareChain;

/**
 * route middleware
 *
 * @author biezhi
 *         2017/5/31
 */
@FunctionalInterface
public interface RouteHandler extends Middleware {

    void handle(Request request, Response response);

    @Override
    default void handle(Invoker invoker, MiddlewareChain middlewareChain) throws Exception {
        handle(invoker.request(), invoker.response());
    }

}
