package com.blade.mvc.http;

import java.util.HashMap;
import java.util.Map;

/**
 * @author biezhi
 *         2017/5/31
 */
public class HttpSession implements Session {

    private Map<String, Object> attrs = new HashMap<>();

    @Override
    public <T> T attribute(String name) {
        return null;
    }

    @Override
    public void attribute(String name, Object value) {

    }

    @Override
    public Map<String, Object> attributes() {
        return attrs;
    }

    @Override
    public void removeAttribute(String name) {

    }
}
