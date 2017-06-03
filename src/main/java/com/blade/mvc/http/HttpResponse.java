package com.blade.mvc.http;

import com.blade.Blade;
import com.blade.BladeException;
import com.blade.kit.DateKit;
import com.blade.kit.JsonKit;
import com.blade.kit.StringKit;
import com.blade.metric.WebStatistics;
import com.blade.mvc.Const;
import com.blade.mvc.WebContext;
import com.blade.mvc.ui.ModelAndView;
import com.blade.mvc.ui.template.TemplateEngine;
import com.blade.server.ProgressiveFutureListener;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.DefaultFileRegion;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.ServerCookieEncoder;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.io.Writer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static io.netty.handler.codec.http.HttpHeaders.Names.*;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

/**
 * @author biezhi
 *         2017/5/31
 */
public class HttpResponse implements Response {

    private static final Logger log = LoggerFactory.getLogger(HttpResponse.class);

    private ChannelHandlerContext ctx;

    private String contentType = Const.CONTENT_TYPE_HTML;
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
    public int statusCode() {
        return this.statusCode;
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
        if (cookie.maxAge() > 0) {
            nettyCookie.setMaxAge(cookie.maxAge());
        }
        nettyCookie.setPath(cookie.path());
        nettyCookie.setHttpOnly(cookie.httpOnly());
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
    public Map<String, String> cookies() {
        Map<String, String> map = new HashMap<>();
        this.cookies.forEach(cookie -> map.put(cookie.name(), cookie.value()));
        return map;
    }

    @Override
    public void text(String text) {
        FullHttpResponse response = new DefaultFullHttpResponse(httpVersion, HttpResponseStatus.valueOf(statusCode), Unpooled.copiedBuffer(text, CharsetUtil.UTF_8));
        this.contentType = Const.CONTENT_TYPE_TEXT;
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
        if (!WebContext.request().isIE()) {
            this.contentType = Const.CONTENT_TYPE_JSON;
        }
        this.send(response);
    }

    @Override
    public void json(Object bean) {
        this.json(JsonKit.toString(bean));
    }

    @Override
    public void donwload(String fileName, File file) throws Exception {
        try {
            if (null == file || !file.exists() || !file.isFile()) {
                throw new BladeException("please check the file is effective!");
            }
            RandomAccessFile raf = new RandomAccessFile(file, "r");
            long fileLength = raf.length();
            this.contentType = StringKit.mimeType(file.getName());

            io.netty.handler.codec.http.HttpResponse httpResponse = new DefaultHttpResponse(HTTP_1_1, OK);
            HttpHeaders httpHeaders = httpResponse.headers().add(getDefaultHeader());

            boolean keepAlive = WebContext.request().keepAlive();
            if (keepAlive) {
                httpResponse.headers().set(CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
            }
            httpHeaders.set(CONTENT_TYPE, this.contentType);
            httpHeaders.set("Content-Disposition", "attachment; filename=" + new String(fileName.getBytes("UTF-8"), "ISO8859_1"));
            httpHeaders.set(CONTENT_LENGTH, fileLength);

            // Write the initial line and the header.
            ctx.write(httpResponse);

            ChannelFuture sendFileFuture = ctx.write(new DefaultFileRegion(raf.getChannel(), 0, fileLength), ctx.newProgressivePromise());
            // Write the end marker.
            ChannelFuture lastContentFuture = ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);

            sendFileFuture.addListener(ProgressiveFutureListener.build(raf));
            // Decide whether to close the connection or not.
            if (!keepAlive) {
                lastContentFuture.addListener(ChannelFutureListener.CLOSE);
            }
            isCommit = true;
        } catch (Exception e) {
            throw e;
        }
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

    @Override
    public void redirect(String newUri) {
        headers.set(HttpHeaders.Names.LOCATION, newUri);
        FullHttpResponse response = new DefaultFullHttpResponse(httpVersion, HttpResponseStatus.FOUND);
        this.send(response);
        if (WebContext.blade().openMonitor()) {
            WebStatistics.me().registerRedirect(newUri);
        }
    }

    @Override
    public boolean isCommit() {
        return isCommit;
    }

    private void send(FullHttpResponse response) {
        response.headers().add(getDefaultHeader());
        boolean keepAlive = WebContext.request().keepAlive();
        if (!keepAlive) {
            // If keep-alive is off, close the connection once the content is fully written.
            ctx.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
        } else {
            // Add 'Content-Length' header only for a keep-alive connection.
            response.headers().setInt(CONTENT_LENGTH, response.content().readableBytes());
            // Add keep alive header as per:
            // - http://www.w3.org/Protocols/HTTP/1.1/draft-ietf-http-v11-spec-01.html#Connection
            response.headers().set(CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
            ctx.writeAndFlush(response);
        }
        isCommit = true;
    }

    private HttpHeaders getDefaultHeader() {
        headers.set(DATE, DateKit.gmtDate());
        headers.set(CONTENT_TYPE, this.contentType);
        headers.set(SERVER, "blade/" + Blade.VER);
        this.cookies.forEach(cookie -> headers.add(SET_COOKIE, ServerCookieEncoder.LAX.encode(cookie)));
        return headers;
    }
}