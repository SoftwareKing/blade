package com.blade.mvc.http;

import com.blade.BladeException;
import com.blade.kit.PathKit;
import com.blade.kit.StringKit;
import com.blade.kit.WebKit;
import com.blade.mvc.multipart.FileItem;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;
import io.netty.handler.codec.http.multipart.*;
import io.netty.util.CharsetUtil;

import java.io.IOException;
import java.util.*;

/**
 * @author biezhi
 *         2017/5/31
 */
public class HttpRequest implements Request {

    private static final HttpDataFactory HTTP_DATA_FACTORY = new DefaultHttpDataFactory(true);

    private ByteBuf body;

    private String contextPath;

    private FullHttpRequest fullHttpRequest;

    private Map<String, String> headers = new HashMap<>();
    private Map<String, Object> attrs = new HashMap<>();
    private Map<String, List<String>> parameters = new HashMap<>();
    private Map<String, String> pathParams = new HashMap<>();
    private Map<String, Cookie> cookies = new HashMap<>();

    private Map<String, FileItem> fileItems = new HashMap<>();

    public HttpRequest(FullHttpRequest fullHttpRequest) {
        this.fullHttpRequest = fullHttpRequest;
        this.init();
    }

    private void init() {
        // 初始化header信息
        fullHttpRequest.headers().forEach((header) -> headers.put(header.getKey(), header.getValue()));
        // 初始化body
        this.body = fullHttpRequest.content().copy();
        // 初始化请求参数
        this.parameters.putAll(new QueryStringDecoder(fullHttpRequest.uri(), CharsetUtil.UTF_8).parameters());
        if (!fullHttpRequest.method().name().equals("GET")) {
            // 是POST请求
            HttpPostRequestDecoder decoder = new HttpPostRequestDecoder(HTTP_DATA_FACTORY, fullHttpRequest);
            decoder.getBodyHttpDatas().stream().forEach(this::parseData);
        }

        // 初始化cookie
        header("Cookie").ifPresent(header -> {
            ServerCookieDecoder.LAX.decode(header).forEach(this::parseCookie);
        });
    }

    private void parseData(InterfaceHttpData data) {
        try {
            switch (data.getHttpDataType()) {
                case Attribute:
                    Attribute attribute = (Attribute) data;
                    String name = attribute.getName();
                    String value = attribute.getValue();
                    this.parameters.put(data.getName(), Arrays.asList(value));
                    break;
                case FileUpload:
                    FileUpload fileUpload = (FileUpload) data;
                    if (fileUpload.isCompleted()) {
                        FileItem fileItem = new FileItem(fileUpload.getName(), fileUpload.getFilename(),
                                fileUpload.getContentType(), fileUpload.length(), fileUpload.getFile());
                        fileItems.put(fileUpload.getName(), fileItem);
                    }
                    break;
                default:
                    break;
            }
        } catch (IOException e) {
            throw new BladeException(e);
        } finally {
            data.release();
        }
    }

    private void parseCookie(io.netty.handler.codec.http.cookie.Cookie nettyCookie) {
        Cookie cookie = new Cookie();
        cookie.name(nettyCookie.name());
        cookie.value(nettyCookie.value());
        cookie.httpOnly(nettyCookie.isHttpOnly());
        cookie.path(nettyCookie.path());
        cookie.domain(nettyCookie.domain());
        cookie.maxAge(nettyCookie.maxAge());
        this.cookies.put(cookie.name(), cookie);
    }

    @Override
    public Request pathParams(Map<String, String> pathParams) {
        if (null != pathParams) {
            this.pathParams = pathParams;
        }
        return this;
    }

    @Override
    public String host() {
        return null;
    }

    @Override
    public String path() {
        return PathKit.getRelativePath(uri(), "/");
    }

    @Override
    public String uri() {
        return fullHttpRequest.uri();
    }

    @Override
    public String userAgent() {
        return header("User-Agent").get();
    }

    @Override
    public String protocol() {
        return fullHttpRequest.protocolVersion().text();
    }

    @Override
    public String contextPath() {
        return "/";
    }

    @Override
    public Map<String, String> pathParams() {
        return this.pathParams;
    }

    @Override
    public String pathString(String name) {
        return this.pathParams.get(name);
    }

    @Override
    public Integer pathInt(String name) {
        String val = pathString(name);
        return StringKit.isNotBlank(val) ? Integer.valueOf(val) : null;
    }

    @Override
    public Long pathLong(String name) {
        String val = pathString(name);
        return StringKit.isNotBlank(val) ? Long.valueOf(val) : null;
    }

    @Override
    public Optional<String> queryString() {
        return null;
    }

    @Override
    public Map<String, List<String>> querys() {
        return parameters;
    }

    @Override
    public Optional<String> query(String name) {
        List<String> values = parameters.get(name);
        if (null != values && values.size() > 0)
            return Optional.of(values.get(0));
        return Optional.empty();
    }

    @Override
    public String query(String name, String defaultValue) {
        Optional<String> value = query(name);
        if (value.isPresent())
            return value.get();
        return defaultValue;
    }

    @Override
    public Optional<Integer> queryInt(String name) {
        Optional<String> value = query(name);
        if (value.isPresent())
            return Optional.of(Integer.valueOf(value.get()));
        return Optional.empty();
    }

    @Override
    public int queryInt(String name, int defaultValue) {
        Optional<String> value = query(name);
        if (value.isPresent())
            return Integer.valueOf(value.get());
        return defaultValue;
    }

    @Override
    public Optional<Long> queryLong(String name) {
        Optional<String> value = query(name);
        if (value.isPresent())
            return Optional.of(Long.valueOf(value.get()));
        return Optional.empty();
    }

    @Override
    public long queryLong(String name, long defaultValue) {
        Optional<String> value = query(name);
        if (value.isPresent())
            return Long.valueOf(value.get());
        return defaultValue;
    }

    @Override
    public Optional<Double> queryDouble(String name) {
        Optional<String> value = query(name);
        if (value.isPresent())
            return Optional.of(Double.valueOf(value.get()));
        return Optional.empty();
    }

    @Override
    public double queryDouble(String name, double defaultValue) {
        Optional<String> value = query(name);
        if (value.isPresent())
            return Double.valueOf(value.get());
        return defaultValue;
    }

    @Override
    public String method() {
        return fullHttpRequest.method().name();
    }

    @Override
    public HttpMethod httpMethod() {
        return HttpMethod.valueOf(method());
    }

    @Override
    public String address() {
        return WebKit.ipAddr(this);
    }

    @Override
    public Session session() {
        return new HttpSession();
    }

    @Override
    public Session session(boolean create) {
        return null;
    }

    @Override
    public String contentType() {
        return header("Content-Type").orElse("");
    }

    @Override
    public boolean isSecure() {
        return false;
    }

    @Override
    public boolean isAjax() {
        return null != header("x-requested-with") && "XMLHttpRequest".equals(header("x-requested-with"));
    }

    @Override
    public boolean isIE() {
        return userAgent().contains("MSIE");
    }

    @Override
    public Map<String, String> cookies() {
        Map<String, String> map = new HashMap<>(cookies.size());
        this.cookies.forEach((name, cookie) -> map.put(name, cookie.value()));
        return map;
    }

    @Override
    public Optional<String> cookie(String name) {
        Cookie cookie = this.cookies.get(name);
        if (null != cookie) {
            return Optional.of(cookie.value());
        }
        return Optional.empty();
    }

    @Override
    public Optional<Cookie> cookieRaw(String name) {
        return Optional.ofNullable(this.cookies.get(name));
    }

    @Override
    public String cookie(String name, String defaultValue) {
        return cookie(name).isPresent() ? cookie(name).get() : defaultValue;
    }

    @Override
    public Map<String, String> headers() {
        return null;
    }

    @Override
    public Optional<String> header(String name) {
        return Optional.ofNullable(headers.get(name));
    }

    @Override
    public String header(String name, String defaultValue) {
        String value = headers.get(name);
        return null == value ? defaultValue : value;
    }

    @Override
    public boolean keepAlive() {
        return HttpUtil.isKeepAlive(fullHttpRequest);
    }

    @Override
    public Request attribute(String name, Object value) {
        this.attrs.put(name, value);
        return this;
    }

    @Override
    public <T> Optional<T> attribute(String name) {
        return Optional.ofNullable((T) this.attrs.get(name));
    }

    @Override
    public Map<String, Object> attributes() {
        return this.attrs;
    }

    @Override
    public Map<String, FileItem> fileItems() {
        return fileItems;
    }

    @Override
    public Optional<FileItem> fileItem(String name) {
        return Optional.ofNullable(fileItems.get(name));
    }

    @Override
    public ByteBuf body() {
        return this.body;
    }

    @Override
    public String bodyToString() {
        return this.body.toString(CharsetUtil.UTF_8);
    }

}