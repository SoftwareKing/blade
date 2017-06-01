package com.blade;

/**
 * @author biezhi
 *         2017/5/31
 */
public class BladeException extends RuntimeException {

    int statusCode = 200;
    String msg = "Execution faild";

    public BladeException() {
    }

    public BladeException(String msg) {
        this.msg = msg;
    }

    public BladeException(int statusCode, String msg) {
        this.statusCode = statusCode;
        this.msg = msg;
    }

}
