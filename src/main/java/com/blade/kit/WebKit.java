package com.blade.kit;

import com.blade.mvc.http.Request;

import java.util.Optional;

/**
 * @author biezhi
 *         2017/6/2
 */
public final class WebKit {

    private WebKit() {
    }

    /**
     * 根据request对象获取客户端ip地址
     *
     * @param request
     * @return
     */
    public static String ipAddr(Request request) {
        Optional<String> ipAddress = request.header("x-forwarded-for");
        if (!ipAddress.isPresent() || "unknown".equalsIgnoreCase(ipAddress.get())) {
            ipAddress = request.header("Proxy-Client-IP");
        }
        if (!ipAddress.isPresent() || "unknown".equalsIgnoreCase(ipAddress.get())) {
            ipAddress = request.header("WL-Proxy-Client-IP");
        }
        if (!ipAddress.isPresent() || "unknown".equalsIgnoreCase(ipAddress.get())) {
            ipAddress = request.header("X-Real-IP");
        }
        if (!ipAddress.isPresent()) {
            ipAddress = Optional.of("127.0.0.1");
        }
        return ipAddress.get();
    }

}
