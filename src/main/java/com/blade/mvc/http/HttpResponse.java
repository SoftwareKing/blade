package com.blade.mvc.http;

import com.blade.kit.JsonKit;
import com.blade.mvc.WebContext;
import com.blade.mvc.ui.ModelAndView;
import com.blade.mvc.ui.template.TemplateEngine;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.ServerCookieEncoder;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.io.Writer;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static io.netty.handler.codec.http.HttpHeaderNames.*;

/**
 * @author biezhi
 *         2017/5/31
 */
public class HttpResponse implements Response {

    private static final Logger log = LoggerFactory.getLogger(HttpResponse.class);

    private ChannelHandlerContext ctx;
    private FullHttpResponse response;

    private String contentType = "text/html; charset=UTF-8";
    private HttpVersion httpVersion = HttpVersion.HTTP_1_1;
    private HttpResponseStatus status = HttpResponseStatus.OK;
    private Object content = Unpooled.EMPTY_BUFFER;
    private HttpHeaders headers = new DefaultHttpHeaders();
    private Set<Cookie> cookies = new HashSet<>();

    private int statusCode = 200;
    private boolean isCommit;

    private TemplateEngine templateEngine;

    public HttpResponse(ChannelHandlerContext ctx, TemplateEngine templateEngine) {
        this.ctx = ctx;
        this.templateEngine = templateEngine;
    }

    @Override
    public Response status(int status) {
        this.statusCode = status;
        return this;
    }

    @Override
    public Response badRequest() {
        this.statusCode = 400;
        return this;
    }

    @Override
    public Response unauthorized() {
        this.statusCode = 401;
        return this;
    }

    @Override
    public Response notFound() {
        this.statusCode = 404;
        return this;
    }

    @Override
    public Response contentType(String contentType) {
        this.contentType = contentType;
        return this;
    }

    @Override
    public String contentType() {
        return this.contentType;
    }

    @Override
    public Map<String, String> headers() {
        Map<String, String> map = new HashMap<>(this.headers.size());
        this.headers.forEach(header -> map.put(header.getKey(), header.getValue()));
        return map;
    }

    @Override
    public String header(String name) {
        return this.headers.get(name);
    }

    @Override
    public Response header(String name, String value) {
        this.headers.set(name, value);
        return this;
    }

    @Override
    public Response cookie(com.blade.mvc.http.Cookie cookie) {
        Cookie nettyCookie = new DefaultCookie(cookie.name(), cookie.value());
        if (cookie.domain() != null) {
            nettyCookie.setDomain(cookie.domain());
        }
        nettyCookie.setMaxAge(cookie.maxAge());
        nettyCookie.setPath(cookie.path());
        this.cookies.add(nettyCookie);
        return this;
    }

    @Override
    public Response cookie(String name, String value) {
        this.cookies.add(new io.netty.handler.codec.http.cookie.DefaultCookie(name, value));
        return this;
    }

    @Override
    public Response cookie(String name, String value, int maxAge) {
        Cookie nettyCookie = new DefaultCookie(name, value);
        nettyCookie.setPath("/");
        nettyCookie.setMaxAge(maxAge);
        this.cookies.add(nettyCookie);
        return this;
    }

    @Override
    public Response cookie(String name, String value, int maxAge, boolean secured) {
        Cookie nettyCookie = new DefaultCookie(name, value);
        nettyCookie.setPath("/");
        nettyCookie.setMaxAge(maxAge);
        nettyCookie.setSecure(secured);
        this.cookies.add(nettyCookie);
        return this;
    }

    @Override
    public Response cookie(String path, String name, String value, int maxAge, boolean secured) {
        Cookie nettyCookie = new DefaultCookie(name, value);
        nettyCookie.setMaxAge(maxAge);
        nettyCookie.setSecure(secured);
        nettyCookie.setPath(path);
        this.cookies.add(nettyCookie);
        return this;
    }

    @Override
    public void text(String text) {
        FullHttpResponse response = new DefaultFullHttpResponse(httpVersion, HttpResponseStatus.valueOf(statusCode), Unpooled.copiedBuffer(text, CharsetUtil.UTF_8));
        this.contentType = "text/plain; charset=UTF-8";
        this.send(response);
    }

    @Override
    public void html(String html) {
        FullHttpResponse response = new DefaultFullHttpResponse(httpVersion, HttpResponseStatus.valueOf(statusCode), Unpooled.copiedBuffer(html, CharsetUtil.UTF_8));
        this.send(response);
    }

    @Override
    public void json(String json) {
        FullHttpResponse response = new DefaultFullHttpResponse(httpVersion, HttpResponseStatus.valueOf(statusCode), Unpooled.copiedBuffer(json, CharsetUtil.UTF_8));
        String userAgent = WebContext.request().userAgent();
        if (!userAgent.contains("MSIE")) {
            this.contentType = "application/json; charset=UTF-8";
        }
        this.send(response);
    }

    @Override
    public void json(Object bean) {
        this.json(JsonKit.toString(bean));
    }

    @Override
    public void render(String view) {
        this.render(new ModelAndView(view));
    }

    @Override
    public void render(ModelAndView modelAndView) {
        ByteBuf buffer = Unpooled.buffer();
        Writer writer = new PrintWriter(new ByteBufOutputStream(buffer));
        templateEngine.render(modelAndView, writer);
        FullHttpResponse response = new DefaultFullHttpResponse(httpVersion, HttpResponseStatus.valueOf(statusCode), buffer);
        this.send(response);
    }

    private void send(FullHttpResponse response) {
        headers.set("Date", gmtDate());
        headers.set("Content-Type", this.contentType);
        headers.setInt("Content-Length", response.content().readableBytes());

        HttpHeaders httpHeaders = response.headers().add(this.headers);
        this.cookies.forEach(cookie -> httpHeaders.add(SET_COOKIE.toString(), ServerCookieEncoder.LAX.encode(cookie)));
        boolean keepAlive = WebContext.request().keepAlive();
        if (!keepAlive) {
            ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
        } else {
            response.headers().set("Connection", KEEP_ALIVE);
            ctx.writeAndFlush(response);
        }
        isCommit = true;
    }


    public static final String HTTP_DATE_FORMAT = "EEE, dd MMM yyyy HH:mm:ss zzz";

    public String gmtDate() {
        ZonedDateTime utc = ZonedDateTime.now(ZoneId.of("GMT"));
        return DateTimeFormatter.ofPattern(HTTP_DATE_FORMAT, Locale.US).format(utc);
    }

    @Override
    public void redirect(String newUri) {
        response.headers().set(HttpHeaders.Names.LOCATION, newUri);
    }

    @Override
    public boolean isCommit() {
        return isCommit;
    }

}