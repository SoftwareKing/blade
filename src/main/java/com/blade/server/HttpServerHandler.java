package com.blade.server;

import com.blade.Blade;
import com.blade.BladeException;
import com.blade.kit.StringKit;
import com.blade.metric.Connection;
import com.blade.metric.WebStatistics;
import com.blade.mvc.RouteHandler;
import com.blade.mvc.WebContext;
import com.blade.mvc.handler.RouteViewResolve;
import com.blade.mvc.http.HttpRequest;
import com.blade.mvc.http.HttpResponse;
import com.blade.mvc.http.Request;
import com.blade.mvc.http.Response;
import com.blade.mvc.route.Route;
import com.blade.mvc.route.RouteMatcher;
import com.blade.mvc.ui.DefaultUI;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static com.blade.mvc.Const.CONTENT_TYPE_TEXT;
import static com.blade.mvc.Const.ENV_KEY_MONITOR_ENABLE;
import static io.netty.handler.codec.http.HttpHeaders.Names.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static io.netty.handler.codec.http.HttpUtil.is100ContinueExpected;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

/**
 * @author biezhi
 *         2017/5/31
 */
@ChannelHandler.Sharable
public class HttpServerHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    public static final Logger log = LoggerFactory.getLogger(HttpServerHandler.class);

    private final Blade blade;
    private final RouteMatcher routeMatcher;
    private final RouteViewResolve routeViewResolve;
    private final Set<String> statics;

    private final StaticFileHandler staticFileHandler;
    private final SessionHandler sessionHandler;

    private final Connection ci;
    private final boolean openMonitor;

    public HttpServerHandler(Blade blade, Connection ci) {
        this.blade = blade;
        this.statics = blade.getStatics();

        this.ci = ci;
        this.openMonitor = blade.environment().getBoolean(ENV_KEY_MONITOR_ENABLE, true);

        this.routeMatcher = blade.routeMatcher();
        this.routeViewResolve = new RouteViewResolve(blade);
        this.staticFileHandler = new StaticFileHandler(blade);
        this.sessionHandler = blade.sessionManager() != null ? new SessionHandler(blade.sessionManager(), blade.environment()) : null;
    }

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        super.channelRegistered(ctx);
        if (openMonitor) {
            WebStatistics.me().addChannel(ctx.channel());
        }
    }

    private FullHttpRequest fullHttpRequest;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest fullHttpRequest) throws Exception {

        this.fullHttpRequest = fullHttpRequest;

        if (is100ContinueExpected(fullHttpRequest)) {
            ctx.write(new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.CONTINUE));
        }

        Request request = HttpRequest.build(ctx, fullHttpRequest, sessionHandler);
        Response response = HttpResponse.build(ctx, blade.templateEngine());

        // reuqest uri
        String uri = request.uri();
        log.debug("{}\t{}\t{}", request.protocol(), request.method(), uri);

        if (isStaticFile(uri)) {
            staticFileHandler.handle(ctx, request, response);
            return;
        }

        // write session
        WebContext.set(new WebContext(request, response));

        // web hook
        if (!invokeHook(routeMatcher.getBefore(uri), request, response)) {
            return;
        }

        Route route = routeMatcher.lookupRoute(request.method(), uri);
        if (null == route) {
            // 404
            sendError(ctx, NOT_FOUND, String.format(DefaultUI.VIEW_404, uri));
            return;
        }
        request.initPathParams(route.getPathParams());
        // execute
        this.routeHandle(request, response, route);

        invokeHook(routeMatcher.getAfter(uri), request, response);

        this.sendFinish(response);
        WebContext.remove();
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
        if (openMonitor) {
            WebStatistics.me().registerRequestFromIp(WebStatistics.getIpFromChannel(ctx.channel()), LocalDateTime.now());
            if (fullHttpRequest != null) {
                ci.addUri(fullHttpRequest.getUri());
            }
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        fullHttpRequest = null;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("error", cause);

        if (!ctx.channel().isActive()) {
            ctx.close();
            return;
        }
        if (cause instanceof BladeException) {
            String error = cause.getMessage();
            Response response = WebContext.response();
            boolean devMode = blade.devMode();
            String contentType = null != response ? response.contentType() : CONTENT_TYPE_TEXT;
            if (!devMode || !contentType.contains("html")) {
                sendError(ctx, INTERNAL_SERVER_ERROR, error);
            }
            StringWriter sw = new StringWriter();
            PrintWriter writer = new PrintWriter(sw);
            writer.write(String.format(DefaultUI.ERROR_START, cause.getClass() + " : " + cause.getMessage()));
            writer.write("\r\n");
            cause.printStackTrace(writer);
            writer.println(DefaultUI.HTML_FOOTER);
            error = sw.toString();
            sendError(ctx, INTERNAL_SERVER_ERROR, error);
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
                HTTP_1_1, status, Unpooled.wrappedBuffer(content.getBytes(CharsetUtil.UTF_8)));
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
     * invoke hooks
     *
     * @param hooks
     * @param request
     * @param response
     * @return
     */
    private boolean invokeHook(List<Route> hooks, Request request, Response response) {
        for (Route route : hooks) {
            if (route.getTargetType() == RouteHandler.class) {
                RouteHandler routeHandler = (RouteHandler) route.getTarget();
                routeHandler.handle(request, response);
            } else {
                boolean flag = routeViewResolve.invokeHook(request, response, route);
                if (!flag) return false;
            }
        }
        return true;
    }

    private void sendFinish(Response response) {
        if (response.isCommit()) {
            return;
        }
        response.body(Unpooled.EMPTY_BUFFER);
    }

}