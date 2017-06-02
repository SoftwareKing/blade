package com.blade;

import com.blade.ioc.Ioc;
import com.blade.ioc.SimpleIoc;
import com.blade.lifecycle.Event;
import com.blade.lifecycle.EventListener;
import com.blade.lifecycle.EventManager;
import com.blade.mvc.RouteHandler;
import com.blade.mvc.http.HttpMethod;
import com.blade.mvc.route.RouteMatcher;
import com.blade.mvc.ui.template.DefaultEngine;
import com.blade.mvc.ui.template.TemplateEngine;
import com.blade.server.netty.BladeServer;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

/**
 * Blade Core
 *
 * @author biezhi
 *         2017/5/31
 */
public class Blade {

    public static final String VER = "2.0.0-SNAPSHOT";

    private String appName = "blade";
    private int port = 9000;
    private boolean fileList = false;
    private boolean gzipEnable = false;
    private boolean started = false;
    private boolean devMode = true;

    private String address = "0.0.0.0";
    private String bootConf = "classpath:app.properties";
    private RouteMatcher routeMatcher = new RouteMatcher();
    private BladeServer bladeServer;

    private Optional<String[]> pkgs = Optional.empty();

    private EventManager eventManager = new EventManager();
    private TemplateEngine templateEngine = new DefaultEngine();
    private Ioc ioc = new SimpleIoc();

    private Set<String> statics = new HashSet<>(Arrays.asList("/favicon.ico", "/static/", "/upload/", "/webjars/"));

    private CountDownLatch latch = new CountDownLatch(1);

    private Blade() {
    }

    private static final Blade INSTANCE = new Blade();

    public static Blade me() {
        return INSTANCE;
    }

    public Ioc ioc() {
        return ioc;
    }

    public Blade get(String path, RouteHandler routeHandler) {
        routeMatcher.addRoute(path, routeHandler, HttpMethod.GET);
        return this;
    }

    public Blade post(String path, RouteHandler routeHandler) {
        routeMatcher.addRoute(path, routeHandler, HttpMethod.POST);
        return this;
    }

    public Blade put(String path, RouteHandler routeHandler) {
        routeMatcher.addRoute(path, routeHandler, HttpMethod.PUT);
        return this;
    }

    public Blade delete(String path, RouteHandler routeHandler) {
        routeMatcher.addRoute(path, routeHandler, HttpMethod.DELETE);
        return this;
    }

    public Blade before(String path, RouteHandler routeHandler) {
        routeMatcher.addRoute(path, routeHandler, HttpMethod.BEFORE);
        return this;
    }

    public Blade after(String path, RouteHandler routeHandler) {
        routeMatcher.addRoute(path, routeHandler, HttpMethod.AFTER);
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
        this.fileList = fileList;
        return this;
    }

    public boolean showFileList() {
        return this.fileList;
    }

    public boolean gzip() {
        return gzipEnable;
    }

    public Blade gzip(boolean gzipEnable) {
        this.gzipEnable = gzipEnable;
        return this;
    }

    public boolean devMode() {
        return this.devMode;
    }

    public Blade devMode(boolean devMode) {
        this.devMode = devMode;
        return this;
    }

    public Set<String> getStatics() {
        return statics;
    }

    public Blade scanPackages(String... pkgs) {
        this.pkgs = Optional.of(pkgs);
        return this;
    }

    public Optional<String[]> scanPackages() {
        return pkgs;
    }

    public Blade bootConf(String bootConf) {
        this.bootConf = bootConf;
        return this;
    }

    public String bootConf() {
        return this.bootConf;
    }

    public Blade listen(int port) {
        this.port = port;
        return this;
    }

    public Blade listen(String address, int port) {
        this.address = address;
        this.port = port;
        return this;
    }

    public String address() {
        return this.address;
    }

    public int port() {
        return this.port;
    }

    public Blade appName(String appName) {
        this.appName = appName;
        return this;
    }

    public String appName() {
        return appName;
    }

    public Blade event(Event.Type eventType, EventListener eventListener) {
        eventManager.addEventListener(eventType, eventListener);
        return this;
    }

    public EventManager eventManager() {
        return eventManager;
    }

    public Blade start() {
        return this.start(null, address, port, null);
    }

    public Blade start(Class<?> mainCls, String... args) {
        return this.start(mainCls, address, port, args);
    }

    public Blade start(Class<?> mainCls, String address, int port, String... args) {
        eventManager.fireEvent(Event.Type.SERVER_STARTING, this);
        bladeServer = new BladeServer(this, args);
        bladeServer.initAndStart(mainCls);
        started = true;
        return this;
    }

    public Blade await() {
        if (!started) {
            throw new IllegalStateException("Server hasn't been started. Call start() before calling this method.");
        }
        try {
            latch.await();
        } catch (Exception e) {
            e.printStackTrace();
            Thread.currentThread().interrupt();
        }
        return this;
    }

    public void stop() {
        eventManager.fireEvent(Event.Type.SERVER_STOPPING, this);
        bladeServer.stop();
        eventManager.fireEvent(Event.Type.SERVER_STOPPED, this);
    }

}