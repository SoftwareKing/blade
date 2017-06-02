package com.example.blog.controller;

import com.blade.ioc.annotation.Inject;
import com.blade.mvc.http.Response;
import com.blade.mvc.annotation.Path;
import com.blade.mvc.annotation.Route;
import com.example.blog.service.AService;

/**
 * @author biezhi
 *         2017/5/31
 */
@Path
public class IndexController {

    @Inject
    private AService aService;

    @Route(values = "/hello")
    public void index(Response response) {
        aService.sayHi();
        response.text("hello world!");
    }

    @Route(values = "/user")
    public String userPage() {
        return "user.html";
    }
}
