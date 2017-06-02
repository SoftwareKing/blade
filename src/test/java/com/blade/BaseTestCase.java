package com.blade;

import com.blade.mvc.RouteHandler;
import com.blade.mvc.http.HttpMethod;
import com.github.kevinsawicki.http.HttpRequest;
import com.mashape.unirest.http.Unirest;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import java.util.concurrent.TimeUnit;

/**
 * @author biezhi
 *         2017/6/3
 */
public class BaseTestCase {

    protected static RouteHandler OK_HANDLER = (req, res) -> res.text("OK");

    protected static Blade app;

    protected static String origin = "http://127.0.0.1:9000";

    @BeforeClass
    public static void setup() throws Exception {
        app = Blade.me()
                .before("/.*", (req, res) -> {
                    System.out.println("before...");
                })
                .start().await();
        TimeUnit.SECONDS.sleep(1);
    }

    @After
    public void clearRoutes() {
        app.routeMatcher().clear();
    }

    @AfterClass
    public static void tearDown() {
        app.stop();
        app.await();
    }

    protected static String getBody(String pathname) throws Exception {
//        return Unirest.get(origin + pathname).asString().getBody();
        return HttpRequest.get(origin + pathname).body();
    }

    protected static String call(HttpMethod method, String pathname) throws Exception {
        if (method == HttpMethod.POST) {
            return HttpRequest.post(origin + pathname).body();
        }
        if (method == HttpMethod.PUT) {
            return HttpRequest.put(origin + pathname).body();
        }
        if (method == HttpMethod.DELETE) {
            return HttpRequest.delete(origin + pathname).body();
        }
        return getBody(origin + pathname);
    }

}
