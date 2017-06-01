package com.blade.mvc.route;

import com.blade.http.HttpMethod;
import com.blade.http.Request;
import com.blade.http.Response;
import com.blade.kit.ReflectKit;
import com.blade.mvc.annotation.Intercept;
import com.blade.mvc.annotation.Path;
import com.blade.mvc.interceptor.Interceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;

/**
 * Route builder
 *
 * @author <a href="mailto:biezhi.me@gmail.com" target="_blank">biezhi</a>
 * @since 1.5
 */
public class RouteBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(RouteBuilder.class);

    private RouteMatcher routeMatcher;

    public RouteBuilder(RouteMatcher routeMatcher) {
        this.routeMatcher = routeMatcher;
    }

    /**
     * Parse Interceptor
     *
     * @param interceptor resolve the interceptor class
     */
    public void addInterceptor(final Class<?> interceptor) {

        boolean hasInterface = ReflectKit.hasInterface(interceptor, Interceptor.class);
        if (null == interceptor || !hasInterface) {
            return;
        }

        Intercept intercept = interceptor.getAnnotation(Intercept.class);
        String partten = "/.*";
        if (null != intercept) {
            partten = intercept.value();
        }

        Method before = ReflectKit.getMethod(interceptor, "before", Request.class, Response.class);
        Method after = ReflectKit.getMethod(interceptor, "after", Request.class, Response.class);
        buildRoute(interceptor, before, partten, HttpMethod.BEFORE);
        buildRoute(interceptor, after, partten, HttpMethod.AFTER);

    }

    /**
     * Parse all routing in a controller
     *
     * @param router resolve the routing class
     */
    public void addRouter(final Class<?> router) {

        Method[] methods = router.getMethods();
        if (null == methods || methods.length == 0) {
            return;
        }
        String nameSpace = null, suffix = null;

        if (null != router.getAnnotation(Path.class)) {
            nameSpace = router.getAnnotation(Path.class).value();
            suffix = router.getAnnotation(Path.class).suffix();
        }

        if (null == nameSpace) {
            LOGGER.warn("Route [{}] not controller annotation", router.getName());
            return;
        }
        for (Method method : methods) {
            com.blade.mvc.annotation.Route mapping = method.getAnnotation(com.blade.mvc.annotation.Route.class);
            //route method
            if (null != mapping) {
                // build multiple route
                HttpMethod methodType = mapping.method();
                String[] paths = mapping.values();
                if (paths.length > 0) {
                    for (String path : paths) {
                        String pathV = getRoutePath(path, nameSpace, suffix);
                        this.buildRoute(router, method, pathV, methodType);
                    }
                }
            }
        }
    }

    private String getRoutePath(String value, String nameSpace, String suffix) {
        String path = value.startsWith("/") ? value : "/" + value;
        nameSpace = nameSpace.startsWith("/") ? nameSpace : "/" + nameSpace;
        path = nameSpace + path;
        path = path.replaceAll("[/]+", "/");
        path = path.length() > 1 && path.endsWith("/") ? path.substring(0, path.length() - 1) : path;
        path = path + suffix;
        return path;
    }

    /**
     * Build a route
     *
     * @param clazz      route target execution class
     * @param execMethod route execution method
     * @param path       route path
     * @param method     route httpmethod
     */
    private void buildRoute(Class<?> clazz, Method execMethod, String path, HttpMethod method) {
        routeMatcher.addRoute(method, path, ReflectKit.newInstance(clazz), clazz, execMethod);
    }

}