package com.example.blog.controller;

import com.blade.mvc.http.Response;
import com.blade.mvc.annotation.Path;
import com.blade.mvc.annotation.Route;

/**
 * @author biezhi
 *         2017/5/31
 */
@Path
public class IndexController {

    @Route(values = "/hello")
    public void index(Response response) {
        response.text("hello world!");
    }

    @Route(values = "/user")
    public String userPage() {
        return "user.html";
    }
}
