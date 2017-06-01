package com.blade.config;

import com.blade.kit.BladeKit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * @author biezhi
 *         2017/6/1
 */
public class Environment {

    private static final Logger log = LoggerFactory.getLogger(Environment.class);

    private Properties props = new Properties();

    private Environment() {
    }

    public static Environment load() {
        return load("app.properties");
    }

    public static Environment load(String location) {
        if (location.startsWith("classpath:")) {
            location = location.substring("classpath:".length());
            return new Environment().loadClasspath(location);
        } else if (location.startsWith("file:")) {
            location = location.substring("file:".length());
            return new Environment().load(new File(location));
        } else if (location.startsWith("url:")) {
            location = location.substring("url:".length());
            try {
                return new Environment().load(new URL(location));
            } catch (MalformedURLException e) {
                log.error("", e);
                return null;
            }
        } else {
            return new Environment().loadClasspath(location);
        }
    }

    // 从 URL 载入
    public Environment load(URL url) {
        String location = url.getPath();
        try {
            location = URLDecoder.decode(location, "utf-8");
            return loadInputStream(url.openStream(), location);
        } catch (UnsupportedEncodingException e) {
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        return null;
    }

    // 从 File 载入
    public Environment load(File file) {
        try {
            return loadInputStream(Files.newInputStream(Paths.get(file.getPath())), file.getName());
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    // 从 classpath 下面载入
    private Environment loadClasspath(String classpath) {
        if (classpath.startsWith("/")) {
            classpath = classpath.substring(1);
        }
        InputStream is = getDefault().getResourceAsStream(classpath);
        return loadInputStream(is, classpath);
    }

    private Environment loadInputStream(InputStream is, String location) {
        if (is == null) {
            log.warn("InputStream not found: " + location);
            return new Environment();
        }
        try {
            this.props.load(is);
            return this;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } finally {
            BladeKit.closeQuietly(is);
        }
    }


    /**
     * Returns current thread's context class loader
     */
    public static ClassLoader getDefault() {
        ClassLoader loader = null;
        try {
            loader = Thread.currentThread().getContextClassLoader();
        } catch (Exception e) {
        }
        if (loader == null) {
            loader = Environment.class.getClassLoader();
            if (loader == null) {
                try {
                    // getClassLoader() returning null indicates the bootstrap ClassLoader
                    loader = ClassLoader.getSystemClassLoader();
                } catch (Exception e) {
                    // Cannot access system ClassLoader - oh well, maybe the caller can live with null...
                }
            }
        }
        return loader;
    }

    public Environment set(String key, String value) {
        props.setProperty(key, value);
        return this;
    }

    public String get(String key) {
        return props.getProperty(key);
    }

    public String getOrDefault(String key, String defaultValue) {
        return props.getProperty(key, defaultValue);
    }

    public Integer getInt(String key) {
        return Integer.valueOf(get(key));
    }

    public Long getLong(String key) {
        return Long.valueOf(get(key));
    }

}
