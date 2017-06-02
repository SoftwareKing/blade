package com.blade.server.netty;

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
import com.blade.mvc.annotation.Path;
import com.blade.mvc.hook.WebHook;
import com.blade.mvc.route.RouteBuilder;
import com.blade.mvc.route.RouteMatcher;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * @author biezhi
 *         2017/5/31
 */
public class BladeServer {

    private static final Logger log = LoggerFactory.getLogger(BladeServer.class);

    public static final String CLASSPATH = BladeServer.class.getResource("/").getPath();

    private Blade blade;
    private String address;
    private int port;
    private boolean SSL;
    private String[] args;
    private Environment environment;
    private EventLoopGroup bossGroup, workerGroup;
    private Set<String> pkgs = new HashSet<>(Arrays.asList("com.blade.plugin"));

    private RouteBuilder routeBuilder;

    public BladeServer(Blade blade, String[] args) {
        this.blade = blade;
        this.address = blade.address();
        this.port = blade.port();
        this.args = args;
    }

    public void initAndStart(Class<?> mainCls) {
        Thread thread = new Thread(() -> {
            long initStart = System.currentTimeMillis();
            log.info("Blade environment: jdk.version\t\t=> {}", System.getProperty("java.version"));
            log.info("Blade environment: user.dir\t\t\t=> {}", System.getProperty("user.dir"));
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
                log.error("start server error", e);
            }
        });
        thread.setName("blade-start-thread");
        thread.start();
    }

    private void initIoc() {
        RouteMatcher routeMatcher = blade.routeMatcher();
        routeBuilder = new RouteBuilder(routeMatcher);

        pkgs.stream()
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

    private void settingServer(ServerBootstrap bootstrap) {
        bootstrap.option(ChannelOption.TCP_NODELAY, true);
        bootstrap.option(ChannelOption.SO_REUSEADDR, true);
        bootstrap.option(ChannelOption.SO_LINGER, 0);
        bootstrap.childOption(ChannelOption.TCP_NODELAY, true);
        bootstrap.childOption(ChannelOption.SO_REUSEADDR, true);
        bootstrap.childOption(ChannelOption.SO_KEEPALIVE, true);
        bootstrap.childOption(ChannelOption.SO_LINGER, 0);
        bootstrap.childOption(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10_000);
        bootstrap.childOption(ChannelOption.SO_RCVBUF, 1048576);
        bootstrap.childOption(ChannelOption.SO_SNDBUF, 1048576);
        bootstrap.childOption(ChannelOption.WRITE_BUFFER_HIGH_WATER_MARK, 10 * 65536);
        bootstrap.childOption(ChannelOption.WRITE_BUFFER_LOW_WATER_MARK, 2 * 65536);
        bootstrap.childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
    }

    private void startServer(long startTime) throws Exception {
        // Configure SSL.
        SslContext sslCtx = null;
        if (SSL) {
            SelfSignedCertificate ssc = new SelfSignedCertificate();
            sslCtx = SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey()).build();
        } else {
            sslCtx = null;
        }

        // Configure the server.
        this.bossGroup = new NioEventLoopGroup(1);
        this.workerGroup = new NioEventLoopGroup();

        try {
            ServerBootstrap b = new ServerBootstrap();
            b.option(ChannelOption.SO_BACKLOG, 1024);
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new HttpServerInitializer(blade, sslCtx));

            Channel ch = b.bind(address, port).sync().channel();
            String appName = blade.appName();

            log.info("{} initialize successfully, Time elapsed: {} ms.", appName, System.currentTimeMillis() - startTime);
            log.info("Blade start with {}:{}", address, port);
            log.info("Open your web browser and navigate to {}://{}:{}", (SSL ? "https" : "http"), address.replace("0.0.0.0", "127.0.0.1"), port);

            blade.eventManager().fireEvent(Event.Type.SERVER_STARTED, blade);

            ch.closeFuture().sync();
        } catch (Exception e) {
            log.error("Blade start error", e);
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
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
            if (null == clazz.getAnnotation(Bean.class)) {
                blade.register(clazz);
            }
            Object hook = blade.ioc().getBean(clazz);
            routeBuilder.addWebHook(clazz, hook);
        }
        if (ReflectKit.hasInterface(clazz, BeanProcessor.class))
            beanProcessors.add((BeanProcessor) blade.ioc().getBean(clazz));
        if (ReflectKit.hasInterface(clazz, StartedEvent.class))
            startedEvents.add((StartedEvent) blade.ioc().getBean(clazz));
    }

    private void loadConfig() {
        String bootConf = blade.bootConf();
        this.environment = Environment.load(bootConf);
        blade.register(environment);
    }

    private void initConfig(Class<?> mainCls) {
        if (blade.scanPackages().isPresent()) {
            pkgs.addAll(Arrays.asList(blade.scanPackages().get()));
        } else if (null != mainCls) {
            pkgs.add(mainCls.getPackage().getName());
        }
        BannerStarter.printStart();
    }

    public void stop() {
        if (this.bossGroup != null) {
            this.bossGroup.shutdownGracefully();
        }
        if (this.workerGroup != null) {
            this.workerGroup.shutdownGracefully();
        }
    }
}
