package com.blade.mvc.http;

import java.util.HashMap;
import java.util.Map;

/**
 * @author biezhi
 *         2017/5/31
 */
public class HttpSession implements Session {

    private Map<String, Object> attrs = new HashMap<>();
    private String id;
    private long created;
    private long expired;

    public HttpSession(String id) {
        this.id = id;
    }

    @Override
    public String id() {
        return id;
    }

    public void setCreated(long created) {
        this.created = created;
    }

    public void setExpired(long expired) {
        this.expired = expired;
    }

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

    @Override
    public long created() {
        return this.created;
    }

    @Override
    public long expired() {
        return this.expired;
    }
}
