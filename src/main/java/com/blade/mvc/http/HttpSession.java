package com.blade.mvc.http;

import java.util.Set;

/**
 * @author biezhi
 *         2017/5/31
 */
public class HttpSession implements Session {

    @Override
    public <T> T attribute(String name) {
        return null;
    }

    @Override
    public void attribute(String name, Object value) {

    }

    @Override
    public Set<String> attributes() {
        return null;
    }

    @Override
    public void removeAttribute(String name) {

    }
}
