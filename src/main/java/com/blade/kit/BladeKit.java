package com.blade.kit;

import com.blade.http.Request;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author biezhi
 *         2017/5/31
 */
public class BladeKit {

    public static boolean isNotBlank(String str) {
        return null != str && !"".equals(str.trim());
    }

    public static boolean isBlank(String str) {
        return null == str || "".equals(str.trim());
    }

    public static String readToString(String file) throws IOException {
        StringBuffer sbuf = new StringBuffer();
        BufferedReader crunchifyBufferReader = Files.newBufferedReader(Paths.get(file));
        List<String> crunchifyList = crunchifyBufferReader.lines().collect(Collectors.toList());
        crunchifyList.forEach(sbuf::append);
        return sbuf.toString();
    }

    /**
     * 根据request对象获取客户端ip地址
     *
     * @param request
     * @return
     */
    public static String ipAddr(Request request) {
        //ipAddress = this.getRequest().getRemoteAddr();
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

    public static void closeQuietly(Closeable closeable) {
        try {
            closeable.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean isEmpty(Collection<?> c) {
        return null == c || c.isEmpty();
    }

    public static boolean isNotEmpty(Collection<?> c) {
        return null != c && !c.isEmpty();
    }

}
