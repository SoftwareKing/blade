package com.blade.server;

import com.blade.Blade;
import com.blade.Environment;
import com.blade.banner.BannerStarter;
import com.blade.ioc.BeanDefine;
import com.blade.ioc.DynamicContext;
import com.blade.ioc.Ioc;
import com.blade.ioc.OrderComparator;
import com.blade.ioc.annotation.Bean;
import com.blade.ioc.reader.ClassInfo;
import com.blade.kit.BladeKit;
import com.blade.kit.ReflectKit;
import com.blade.lifecycle.BeanProcessor;
import com.blade.lifecycle.Event;
import com.blade.lifecycle.StartedEvent;
import com.blade.mvc.Const;
import com.blade.mvc.annotation.Path;
import com.blade.mvc.hook.WebHook;
import com.blade.mvc.route.RouteBuilder;
import com.blade.mvc.route.RouteMatcher;
import com.blade.mvc.ui.DefaultUI;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.ssl.SslContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static com.blade.mvc.Const.CLASSPATH;

/**
 * @author biezhi
 *         2017/5/31
 */
public class WebServer {

    private static final Logger log = LoggerFactory.getLogger(WebServer.class);

    private Blade blade;
    private Environment environment;

    private EventLoopGroup bossGroup, workerGroup;
    private Channel channel;

    private RouteBuilder routeBuilder;

    public void initAndStart(Blade blade, String[] args, Class<?> mainCls) throws Exception {
        this.blade = blade;

        long initStart = System.currentTimeMillis();
        log.info("Blade environment: jdk.version\t=> {}", System.getProperty("java.version"));
        log.info("Blade environment: user.dir\t\t=> {}", System.getProperty("user.dir"));
        log.info("Blade environment: java.io.tmpdir\t=> {}", System.getProperty("java.io.tmpdir"));
        log.info("Blade environment: user.timezone\t=> {}", System.getProperty("user.timezone"));
        log.info("Blade environment: file.encoding\t=> {}", System.getProperty("file.encoding"));
        log.info("Blade environment: classpath\t\t=> {}", CLASSPATH);

        try {
            // 1. 加载配置
            this.loadConfig();

            // 2. 初始化系统配置
            this.initConfig(mainCls);

            // 3. 初始化ioc
            this.initIoc();

            // 4. 执行beanprocessor、启动事件
            beanProcessors.stream().sorted(new OrderComparator<>()).forEach(b -> b.processor(blade));
            startedEvents.forEach(e -> blade.event(Event.Type.SERVER_STARTED, e));

            // 5. 启动web服务
            this.startServer(initStart);
        } catch (Exception e) {
            throw e;
        }
    }

    private void initIoc() {
        RouteMatcher routeMatcher = blade.routeMatcher();
        routeBuilder = new RouteBuilder(routeMatcher);

        blade.scanPackages().stream()
                .flatMap(DynamicContext::recursionFindClasses)
                .map(ClassInfo::getClazz)
                .filter(ReflectKit::isNormalClass)
                .forEach(this::parseCls);

        routeMatcher.register();

        Ioc ioc = blade.ioc();
        if (BladeKit.isNotEmpty(ioc.getBeans())) {
            log.info("Register bean: {}", ioc.getBeans());
        }

        List<BeanDefine> beanDefines = ioc.getBeanDefines();
        if (BladeKit.isNotEmpty(beanDefines)) {
            beanDefines.forEach(b -> BladeKit.injection(ioc, b));
        }
    }

    private void startServer(long startTime) throws Exception {
        // Configure SSL.
        SslContext sslCtx = null;
        boolean SSL = false;

        // Configure the server.
        this.bossGroup = new NioEventLoopGroup(1);
        this.workerGroup = new NioEventLoopGroup();

        ServerBootstrap b = new ServerBootstrap();
        b.option(ChannelOption.SO_BACKLOG, 1024);
        b.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new HttpServerInitializer(blade, sslCtx));

        String address = blade.address();
        int port = blade.port();

        channel = b.bind(address, port).sync().channel();
        String appName = blade.appName();

        log.info("{} initialize successfully, Time elapsed: {} ms.", appName, System.currentTimeMillis() - startTime);
        log.info("Blade start with {}:{}", address, port);
        log.info("Open your web browser and navigate to {}://{}:{}", (SSL ? "https" : "http"), address.replace("0.0.0.0", "127.0.0.1"), port);

        blade.eventManager().fireEvent(Event.Type.SERVER_STARTED, blade);
    }

    private List<BeanProcessor> beanProcessors = new ArrayList<>();
    private List<StartedEvent> startedEvents = new ArrayList<>();

    private void parseCls(Class<?> clazz) {
        if (null != clazz.getAnnotation(Bean.class)) blade.register(clazz);
        if (null != clazz.getAnnotation(Path.class)) {
            if (null == clazz.getAnnotation(Bean.class)) {
                blade.register(clazz);
            }
            Object controller = blade.ioc().getBean(clazz);
            routeBuilder.addRouter(clazz, controller);
        }
        if (ReflectKit.hasInterface(clazz, WebHook.class)) {
            if (null != clazz.getAnnotation(Bean.class)) {
                Object hook = blade.ioc().getBean(clazz);
                routeBuilder.addWebHook(clazz, hook);
            }
        }
        if (ReflectKit.hasInterface(clazz, BeanProcessor.class))
            beanProcessors.add((BeanProcessor) blade.ioc().getBean(clazz));
        if (ReflectKit.hasInterface(clazz, StartedEvent.class))
            startedEvents.add((StartedEvent) blade.ioc().getBean(clazz));
    }

    private void loadConfig() {
        String bootConf = blade.bootConf();
        this.environment = Environment.of(bootConf);
        blade.register(environment);
    }

    private void initConfig(Class<?> mainCls) {

        if (null != mainCls) {
            if (blade.scanPackages().size() == 1 && blade.scanPackages().contains(Const.PLUGIN_PACKAGE_NAME)) {
                blade.scanPackages(mainCls.getPackage().getName());
            }
        }

        BannerStarter.printStart();

        if (blade.openMonitor()) {
            DefaultUI.registerStatus(blade);
        }

    }

    public void stop() {
        if (this.bossGroup != null) {
            this.bossGroup.shutdownGracefully();
        }
        if (this.workerGroup != null) {
            this.workerGroup.shutdownGracefully();
        }
    }

    public void join() throws InterruptedException {
        try {
            channel.closeFuture().sync();
        } catch (Exception e) {
            this.stop();
        }
    }

}
