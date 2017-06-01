package com.blade.http;

import java.util.Set;

/**
 * Session
 *
 * @author <a href="mailto:biezhi.me@gmail.com" target="_blank">biezhi</a>
 * @since 1.5
 */
public interface Session {

    <T> T attribute(String name);

    void attribute(String name, Object value);

    Set<String> attributes();

    void removeAttribute(String name);

}