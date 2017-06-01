package com.blade.exception;

/**
 * @author biezhi
 *         2017/5/31
 */
public class RouteException extends BladeException {

    public RouteException() {
    }

    public RouteException(String message) {
        super(message);
    }

    public RouteException(String message, Throwable cause) {
        super(message, cause);
    }

    public RouteException(Throwable cause) {
        super(cause);
    }
}
