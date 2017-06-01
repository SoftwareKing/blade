package com.blade.server.netty;

import com.blade.Blade;
import com.blade.banner.BannerStarter;
import com.blade.config.Environment;
import com.blade.ioc.BeanDefine;
import com.blade.ioc.DynamicContext;
import com.blade.ioc.Ioc;
import com.blade.ioc.annotation.Bean;
import com.blade.ioc.annotation.Order;
import com.blade.ioc.reader.ClassInfo;
import com.blade.kit.BladeKit;
import com.blade.kit.ReflectKit;
import com.blade.lifecycle.BeanProcessor;
import com.blade.lifecycle.Event;
import com.blade.lifecycle.StartedEvent;
import com.blade.mvc.annotation.Path;
import com.blade.mvc.route.RouteBuilder;
import com.blade.mvc.route.RouteMatcher;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import org.omg.PortableInterceptor.Interceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
    private Set<String> pkgs = new HashSet<>(Arrays.asList("com.blade"));

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

                // 4. 启动web服务
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
                .flatMap((DynamicContext::recursionFindClasses))
                .map(ClassInfo::getClazz)
                .filter(ReflectKit::isNormalClass)
                .sorted(this::compareTo)
                .forEach(this::parseCls);

        routeMatcher.register();

        Ioc ioc = blade.ioc();
        if (BladeKit.isNotEmpty(ioc.getBeans())) {
            log.info("Register bean: {}", ioc.getBeans());
        }

        List<BeanDefine> beanDefines = ioc.getBeanDefines();
        if (BladeKit.isNotEmpty(beanDefines)) {
            beanDefines.forEach(b -> ReflectKit.injection(ioc, b));
        }
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

    private void parseCls(Class<?> clazz) {
        if (null != clazz.getAnnotation(Bean.class))
            blade.register(clazz);
        if (null != clazz.getAnnotation(Path.class))
            routeBuilder.addRouter(clazz);
        if (ReflectKit.hasInterface(clazz, Interceptor.class))
            routeBuilder.addInterceptor(clazz);
        if (ReflectKit.hasInterface(clazz, BeanProcessor.class))
            ((BeanProcessor) blade.ioc().addBean(clazz)).register(blade.ioc());
        if (ReflectKit.hasInterface(clazz, StartedEvent.class))
            blade.event(Event.Type.SERVER_STARTED, (StartedEvent) blade.ioc().addBean(clazz));
    }

    private int compareTo(Class<?> c1, Class<?> c2) {
        Order o1 = c1.getAnnotation(Order.class);
        Order o2 = c2.getAnnotation(Order.class);
        Integer order1 = null != o1 ? o1.value() : Integer.MAX_VALUE;
        Integer order2 = null != o2 ? o2.value() : Integer.MAX_VALUE;
        return order1.compareTo(order2);
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
