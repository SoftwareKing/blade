package com.blade.server.netty;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Callable;

/**
 * Http响应回调处理
 */
public class HttpResponseCallback<T> implements Callable<T> {

    public static final Logger log = LoggerFactory.getLogger(HttpResponseCallback.class);

    @Override
    public T call() throws Exception {
        return null;
    }
}