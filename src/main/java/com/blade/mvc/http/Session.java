package com.blade.mvc.http;

import java.util.Map;
import java.util.Set;

/**
 * Session
 *
 * @author <a href="mailto:biezhi.me@gmail.com" target="_blank">biezhi</a>
 * @since 1.5
 */
public interface Session {

    String id();

    void attribute(String name, Object value);

    Map<String, Object> attributes();

    void removeAttribute(String name);

    long created();

    long expired();

}