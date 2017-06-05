package com.blade.mvc.middleware;

import com.blade.mvc.hook.Invoker;

/**
 * middleware interface
 *
 * @author biezhi
 *         2017/6/5
 */
@FunctionalInterface
public interface Middleware {

    void handle(Invoker invoker, MiddlewareChain middlewareChain) throws Exception;

}
