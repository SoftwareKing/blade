package com.blade.server.netty;

import com.blade.Blade;
import com.blade.http.HttpRequest;
import com.blade.http.HttpResponse;
import com.blade.http.Request;
import com.blade.http.Response;
import com.blade.ioc.Ioc;
import com.blade.kit.PathKit;
import com.blade.mvc.RouteHandler;
import com.blade.mvc.WebContext;
import com.blade.mvc.handler.RouteViewResolve;
import com.blade.mvc.route.Route;
import com.blade.mvc.route.RouteMatcher;
import com.blade.mvc.ui.template.TemplateEngine;
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

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static io.netty.handler.codec.http.HttpHeaders.Names.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

/**
 * @author biezhi
 *         2017/5/31
 */
@ChannelHandler.Sharable
public class HttpServerHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    public static final Logger log = LoggerFactory.getLogger(HttpServerHandler.class);

    private Ioc ioc;
    private RouteMatcher routeMatcher;
    private RouteViewResolve routeViewResolve;
    private Set<String> statics;
    private boolean showFileList;
    private TemplateEngine templateEngine;
    private StaticFileHandler staticFileHandler;

    private static final ExecutorService threadPool = Executors.newFixedThreadPool(8);

    public HttpServerHandler(Blade blade) {
        this.ioc = blade.ioc();
        this.statics = blade.getStatics();
        this.templateEngine = blade.templateEngine();
        this.routeMatcher = blade.routeMatcher();
        this.routeViewResolve = new RouteViewResolve(blade);
        this.staticFileHandler = new StaticFileHandler(blade);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest fullHttpRequest) throws Exception {
        Request request = new HttpRequest(fullHttpRequest);
        Response response = new HttpResponse(ctx, templateEngine);
        response.header("Server", "blade/" + Blade.VER);

        // reuqest uri
        String uri = PathKit.getRelativePath(request.uri(), request.contextPath());
        log.debug("{}\t{}\t{}", request.protocol(), request.method(), uri);

        // 判断是否是静态资源
        if (isStaticFile(uri)) {
            staticFileHandler.execute(ctx, request, response, uri);
        } else {
            WebContext.set(new WebContext(request, response));
            Route route = routeMatcher.getRoute(request.method(), uri);
            if (null != route) {
                // execute
                HttpServerHandler.this.routeHandle(request, response, route);
            } else {
                // 404
                sendError(ctx, NOT_FOUND);
            }
            WebContext.remove();
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("", cause);
        if (ctx.channel().isActive()) {
            sendError(ctx, INTERNAL_SERVER_ERROR);
        }
    }

    private boolean isStaticFile(String uri) {
        Optional<String> result = statics.stream().filter(s -> s.equals(uri) || uri.startsWith(s)).findFirst();
        return result.isPresent();
    }

    private static void sendError(ChannelHandlerContext ctx, HttpResponseStatus status) {
        FullHttpResponse response = new DefaultFullHttpResponse(
                HTTP_1_1, status, Unpooled.copiedBuffer("Failure: " + status + "\r\n", CharsetUtil.UTF_8));
        response.headers().set(CONTENT_TYPE, "text/plain; charset=UTF-8");
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
    private void routeHandle(Request request, Response response, Route route) throws Exception {
        Object target = route.getTarget();
        if (null == target) {
            Class<?> clazz = route.getAction().getDeclaringClass();
            target = ioc.getBean(clazz);
            route.setTarget(target);
        }
        if (route.getTargetType() == RouteHandler.class) {
            RouteHandler routeHandler = (RouteHandler) target;
            routeHandler.handle(request, response);
        } else {
            routeViewResolve.handle(request, response, route);
        }
    }

}