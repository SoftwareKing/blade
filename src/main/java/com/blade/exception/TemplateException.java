package com.blade.exception;

/**
 * @author biezhi
 *         2017/5/31
 */
public class TemplateException extends Exception {

    public TemplateException() {
    }

    public TemplateException(String message) {
        super(message);
    }

    public TemplateException(String message, Throwable cause) {
        super(message, cause);
    }

    public TemplateException(Throwable cause) {
        super(cause);
    }

}
