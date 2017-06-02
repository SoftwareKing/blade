package com.blade.mvc;

import com.blade.BaseTestCase;
import com.blade.kit.ason.Ason;
import com.github.kevinsawicki.http.HttpRequest;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;


/**
 * Request TestCase
 *
 * @author biezhi
 *         2017/6/3
 */
public class RequestTest extends BaseTestCase {

    @Test
    public void testNullCookie() throws Exception {
        start(app.get("/cookie", (req, res) -> res.text(req.cookie("user-id").orElse("null"))));
        assertThat(getBody("/cookie"), is("null"));
    }

    @Test
    public void testSetCookie() throws Exception {
        start(app.get("/cookie", (req, res) -> res.text(req.cookie("user-id").orElse("null"))));
        String body = HttpRequest.get(origin + "/cookie").header("Cookie", "user-id=221").body();
        assertThat(body, is("221"));
    }

    @Test
    public void testGetCookies() throws Exception {
        start(app.get("/cookie", (req, res) -> res.text(req.cookies().toString())));
        assertThat(getBody("/cookie"), is("{}"));
    }

    @Test
    public void testMultipleCookies() throws Exception {
        start(app.get("/cookie", (req, res) -> res.json(req.cookies())));
        String body = HttpRequest.get(origin + "/cookie").header("Cookie", "c1=a;c2=b;c3=c").body();
        Ason ason = new Ason(body);
        assertThat(ason.get("c1"), is("a"));
        assertThat(ason.get("c2"), is("b"));
        assertThat(ason.get("c3"), is("c"));
    }

    @Test
    public void testPathParam() throws Exception {
        start(
                app.get("/user1/:id", (req, res) -> res.text(req.pathInt("id").toString()))
                        .get("/user2/:id", (req, res) -> res.text(req.pathLong("id").toString()))
                        .get("/user3/:name/:age", (req, res) -> res.text(req.pathString("name") + ":" + req.pathString("age")))
        );
        assertThat(getBody("/user1/24"), is("24"));
        assertThat(getBody("/user2/25"), is("25"));
        assertThat(getBody("/user3/jack/18"), is("jack:18"));
    }

}
