package com.blade.server.netty;

import com.blade.Blade;
import com.blade.BladeException;
import com.blade.ioc.Ioc;
import com.blade.kit.DateKit;
import com.blade.kit.StringKit;
import com.blade.mvc.Const;
import com.blade.mvc.RouteHandler;
import com.blade.mvc.WebContext;
import com.blade.mvc.handler.RouteViewResolve;
import com.blade.mvc.http.*;
import com.blade.mvc.route.Route;
import com.blade.mvc.route.RouteMatcher;
import com.blade.mvc.ui.DefaultUI;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Optional;
import java.util.Set;

import static io.netty.handler.codec.http.HttpHeaders.Names.*;
import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static io.netty.handler.codec.http.HttpUtil.is100ContinueExpected;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

/**
 * @author biezhi
 *         2017/5/31
 */
@ChannelHandler.Sharable
public class HttpServerHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    public static final Logger log = LoggerFactory.getLogger(HttpServerHandler.class);

    private Blade blade;
    private RouteMatcher routeMatcher;
    private RouteViewResolve routeViewResolve;
    private Set<String> statics;

    private StaticFileHandler staticFileHandler;

    private SessionHandler sessionHandler;

    public HttpServerHandler(Blade blade) {
        this.blade = blade;
        this.statics = blade.getStatics();

        this.routeMatcher = blade.routeMatcher();
        this.routeViewResolve = new RouteViewResolve(blade);
        this.staticFileHandler = new StaticFileHandler(blade);
        this.sessionHandler = new SessionHandler(blade.sessionManager());
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest fullHttpRequest) throws Exception {
        if (is100ContinueExpected(fullHttpRequest)) {
            ctx.write(new DefaultFullHttpResponse(HTTP_1_1, CONTINUE));
        }

        Request request = new HttpRequest(ctx, fullHttpRequest);
        Response response = new HttpResponse(ctx, blade.templateEngine());

        // reuqest uri
        String uri = request.uri();
        log.debug("{}\t{}\t{}", request.protocol(), request.method(), uri);

        if (isStaticFile(uri)) {
            staticFileHandler.handle(ctx, request, response);
            return;
        }

        // execute session
        SessionManager sessionManager = sessionHandler.handle(ctx, request, response);

        WebContext.set(new WebContext(sessionManager, request, response));

        // web hook
        int interrupts = routeMatcher.getBefore(uri).stream().mapToInt(route -> this.invokeHook(request, response, route)).sum();
        if (interrupts > 0) return;

        Route route = routeMatcher.lookupRoute(request.method(), uri);
        if (null == route) {
            // 404
            sendError(ctx, NOT_FOUND, String.format(DefaultUI.VIEW_404, uri));
            return;
        }

        request.initPathParams(route.getPathParams());
        // execute
        this.routeHandle(request, response, route);
        routeMatcher.getAfter(uri).forEach(r -> this.invokeHook(request, response, r));
        this.sendFinish(ctx, response);

        WebContext.remove();

    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("", cause);
        if (!ctx.channel().isActive()) {
            ctx.close();
            return;
        }
        if (cause instanceof BladeException) {
            String error = cause.getMessage();
            Response response = WebContext.response();
            boolean devMode = blade.devMode();
            String contentType = null != response ? response.contentType() : Const.CONTENT_TYPE_TEXT;
            if (!devMode || !contentType.contains("html")) {
                sendError(ctx, INTERNAL_SERVER_ERROR, error);
            }
            StringWriter sw = new StringWriter();
            PrintWriter writer = new PrintWriter(sw);
            writer.write(String.format(DefaultUI.HTML, cause.getClass() + " : " + cause.getMessage()));
            writer.write("\r\n");
            cause.printStackTrace(writer);
            writer.println(DefaultUI.END);
            error = sw.toString();
            return;
        }
        sendError(ctx, INTERNAL_SERVER_ERROR);
    }

    private boolean isStaticFile(String uri) {
        Optional<String> result = statics.stream().filter(s -> s.equals(uri) || uri.startsWith(s)).findFirst();
        return result.isPresent();
    }

    private static void sendError(ChannelHandlerContext ctx, HttpResponseStatus status) {
        sendError(ctx, status, "");
    }

    private static void sendError(ChannelHandlerContext ctx, HttpResponseStatus status, String content) {
        boolean isHtml = StringKit.isNotBlank(content);
        content = isHtml ? content : "Failure: " + status + "\r\n";
        FullHttpResponse response = new DefaultFullHttpResponse(
                HTTP_1_1, status, Unpooled.copiedBuffer(content, CharsetUtil.UTF_8));
        response.headers().set(CONTENT_TYPE, isHtml ? "text/html; charset=UTF-8" : "text/plain; charset=UTF-8");
        // Close the connection as soon as the error message is sent.
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }

    /**
     * Actual routing method execution
     *
     * @param request  request object
     * @param response response object
     * @param route    route object
     */
    private boolean routeHandle(Request request, Response response, Route route) {
        Object target = route.getTarget();
        if (null == target) {
            Class<?> clazz = route.getAction().getDeclaringClass();
            target = blade.getBean(clazz);
            route.setTarget(target);
        }
        if (route.getTargetType() == RouteHandler.class) {
            RouteHandler routeHandler = (RouteHandler) target;
            routeHandler.handle(request, response);
            return false;
        } else {
            return routeViewResolve.handle(request, response, route);
        }
    }

    /**
     * Methods to perform the hooks
     *
     * @param request  request object
     * @param response response object
     * @return Return execute is ok
     */
    private int invokeHook(Request request, Response response, Route route) {
        if (route.getTargetType() == RouteHandler.class) {
            RouteHandler routeHandler = (RouteHandler) route.getTarget();
            routeHandler.handle(request, response);
            return 0;
        } else {
            return routeViewResolve.invokeHook(request, response, route) ? 0 : 1;
        }
    }

    private void sendFinish(ChannelHandlerContext ctx, Response response) {
        if (response.isCommit()) {
            return;
        }
        FullHttpResponse httpResponse = new DefaultFullHttpResponse(HTTP_1_1, OK, Unpooled.EMPTY_BUFFER);
        httpResponse.headers().set(CONTENT_TYPE, "text/plain; charset=UTF-8");
        httpResponse.headers().set(DATE, DateKit.gmtDate());
        httpResponse.headers().setInt(CONTENT_LENGTH, 0);
        ctx.writeAndFlush(httpResponse);
    }

}