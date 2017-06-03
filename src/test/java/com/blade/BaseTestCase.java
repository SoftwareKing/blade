package com.blade;

import com.blade.mvc.RouteHandler;
import com.blade.mvc.http.HttpMethod;
import com.github.kevinsawicki.http.HttpRequest;
import org.junit.After;
import org.junit.Before;

/**
 * @author biezhi
 *         2017/6/3
 */
public class BaseTestCase {

    protected RouteHandler OK_HANDLER = (req, res) -> res.text("OK");
    protected Blade app;
    protected String origin = "http://127.0.0.1:9000";
    protected String firefoxUA = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.12; rv:53.0) Gecko/20100101 Firefox/53.0";

    @Before
    public void setup() throws Exception {
        app = Blade.me();
    }

    protected void start(Blade blade) {
        blade.start().await();
    }

    @After
    public void after() {
        app.stop();
        app.await();
    }

    protected String getBody(String pathname) throws Exception {
        return HttpRequest.get(origin + pathname).body();
    }

    protected String call(HttpMethod method, String pathname) throws Exception {
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
