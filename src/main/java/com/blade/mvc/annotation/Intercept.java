package com.blade.mvc.annotation;

import java.lang.annotation.*;

/**
 * Interceptor notes, written in the class
 * e.g:
 * <pre>
 * {@link Intercept}
 * public class BaseInterceptor {...}
 * </pre>
 *
 * @author <a href="mailto:biezhi.me@gmail.com" target="_blank">biezhi</a>
 * @since 1.5
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Intercept {

    String value() default "/.*";

    int sort() default 0;
}