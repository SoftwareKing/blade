package com.blade.mvc.middleware;

/**
 * @author biezhi
 *         2017/6/5
 */
@FunctionalInterface
public interface MiddlewareChain {

    boolean next() throws Exception;

}
