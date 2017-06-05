package com.blade;

import com.blade.ioc.Ioc;
import com.blade.ioc.SimpleIoc;
import com.blade.event.Event;
import com.blade.event.EventListener;
import com.blade.event.EventManager;
import com.blade.mvc.RouteMiddleware;
import com.blade.mvc.http.HttpMethod;
import com.blade.mvc.http.SessionManager;
import com.blade.mvc.route.RouteMatcher;
import com.blade.mvc.ui.template.DefaultEngine;
import com.blade.mvc.ui.template.TemplateEngine;
import com.blade.server.WebServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;

import static com.blade.mvc.Const.*;

/**
 * Blade Core
 *
 * @author biezhi
 *         2017/5/31
 */
public class Blade {

    private static final Logger log = LoggerFactory.getLogger(Blade.class);

    private boolean started = false;

    private RouteMatcher routeMatcher = new RouteMatcher();
    private WebServer webServer = new WebServer();
    private Class<?> bootClass;

    private Set<String> pkgs = new LinkedHashSet<>(Arrays.asList(PLUGIN_PACKAGE_NAME));

    private Ioc ioc = new SimpleIoc();
    private TemplateEngine templateEngine = new DefaultEngine();

    private Environment environment = Environment.empty();

    private EventManager eventManager = new EventManager();
    private SessionManager sessionManager = new SessionManager();

    private Set<String> statics = new HashSet<>(Arrays.asList("/favicon.ico", "/static/", "/upload/", "/webjars/"));

    private Consumer<Exception> startupExceptionHandler = (e) -> log.error("Failed to start Blade", e);

    private CountDownLatch latch = new CountDownLatch(1);

    private Blade() {
    }

    public static Blade me() {
        return new Blade();
    }

    public Ioc ioc() {
        return ioc;
    }

    public Blade get(String path, RouteMiddleware routeMiddleware) {
        routeMatcher.addRoute(path, routeMiddleware, HttpMethod.GET);
        return this;
    }

    public Blade post(String path, RouteMiddleware routeMiddleware) {
        routeMatcher.addRoute(path, routeMiddleware, HttpMethod.POST);
        return this;
    }

    public Blade put(String path, RouteMiddleware routeMiddleware) {
        routeMatcher.addRoute(path, routeMiddleware, HttpMethod.PUT);
        return this;
    }

    public Blade delete(String path, RouteMiddleware routeMiddleware) {
        routeMatcher.addRoute(path, routeMiddleware, HttpMethod.DELETE);
        return this;
    }

    public Blade before(String path, RouteMiddleware routeMiddleware) {
        routeMatcher.addRoute(path, routeMiddleware, HttpMethod.BEFORE);
        return this;
    }

    public Blade after(String path, RouteMiddleware routeMiddleware) {
        routeMatcher.addRoute(path, routeMiddleware, HttpMethod.AFTER);
        return this;
    }

    public Blade templateEngine(TemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
        return this;
    }

    public TemplateEngine templateEngine() {
        return templateEngine;
    }

    public RouteMatcher routeMatcher() {
        return routeMatcher;
    }

    public Blade register(Object bean) {
        ioc.addBean(bean);
        return this;
    }

    public Blade register(Class<?> cls) {
        ioc.addBean(cls);
        return this;
    }

    public Blade addStatics(String... folders) {
        statics.addAll(Arrays.asList(folders));
        return this;
    }

    public Blade showFileList(boolean fileList) {
        this.environment(ENV_KEY_STATIC_LIST, fileList);
        return this;
    }

    public Blade gzip(boolean gzipEnable) {
        this.environment(ENV_KEY_GZIP_ENABLE, gzipEnable);
        return this;
    }

    public Object getBean(Class<?> cls) {
        return ioc.getBean(cls);
    }

    public boolean devMode() {
        return environment.getBoolean(ENV_KEY_DEV_MODE, true);
    }

    public Blade devMode(boolean devMode) {
        this.environment(ENV_KEY_DEV_MODE, devMode);
        if (!devMode) {
            this.openMonitor(false);
        }
        return this;
    }

    public Class<?> bootClass() {
        return this.bootClass;
    }

    public Blade openMonitor(boolean openMonitor) {
        this.environment(ENV_KEY_GZIP_ENABLE, openMonitor);
        return this;
    }

    public Set<String> getStatics() {
        return statics;
    }

    public Blade scanPackages(String... pkgs) {
        this.pkgs.addAll(Arrays.asList(pkgs));
        return this;
    }

    public Set<String> scanPackages() {
        return pkgs;
    }

    public Blade bootConf(String bootConf) {
        this.environment(ENV_KEY_BOOT_CONF, bootConf);
        return this;
    }

    public Blade environment(String key, Object value) {
        environment.set(key, value);
        return this;
    }

    public Environment environment() {
        return environment;
    }

    public Blade listen(int port) {
        this.environment(ENV_KEY_SERVER_PORT, port);
        return this;
    }

    public Blade listen(String address, int port) {
        this.environment(ENV_KEY_SERVER_ADDRESS, address);
        this.environment(ENV_KEY_SERVER_PORT, port);
        return this;
    }

    public Blade appName(String appName) {
        this.environment(ENV_KEY_APP_NAME, appName);
        return this;
    }

    public Blade event(Event.Type eventType, EventListener eventListener) {
        eventManager.addEventListener(eventType, eventListener);
        return this;
    }

    public EventManager eventManager() {
        return eventManager;
    }

    public SessionManager sessionManager() {
        return sessionManager;
    }

    public Blade closeSessoin() {
        this.sessionManager = null;
        return this;
    }

    public Blade start() {
        return this.start(null, DEFAULT_SERVER_ADDRESS, DEFAULT_SERVER_PORT, null);
    }

    public Blade start(Class<?> mainCls, String... args) {
        return this.start(mainCls, DEFAULT_SERVER_ADDRESS, DEFAULT_SERVER_PORT, args);
    }

    public Blade start(Class<?> bootClass, String address, int port, String... args) {
        try {
            this.bootClass = bootClass;
            eventManager.fireEvent(Event.Type.SERVER_STARTING, this);
            Thread thread = new Thread(() -> {
                try {
                    webServer.initAndStart(Blade.this, args);
                    latch.countDown();
                    webServer.join();
                } catch (Exception e) {
                    startupExceptionHandler.accept(e);
                }
            });
            thread.setName("blade-start-thread");
            thread.start();
            started = true;
        } catch (Exception e) {
            startupExceptionHandler.accept(e);
        }
        return this;
    }

    public Blade await() {
        if (!started) {
            throw new IllegalStateException("Server hasn't been started. Call start() before calling this method.");
        }
        try {
            latch.await();
        } catch (Exception e) {
            log.error("awit error", e);
            Thread.currentThread().interrupt();
        }
        return this;
    }

    public void stop() {
        eventManager.fireEvent(Event.Type.SERVER_STOPPING, this);
        webServer.stop();
        eventManager.fireEvent(Event.Type.SERVER_STOPPED, this);
    }

}