package com.example.blog;

import com.blade.Blade;
import com.blade.kit.JsonKit;

import java.util.HashMap;
import java.util.Map;

/**
 * @author biezhi
 *         2017/5/31
 */
public class Application {

    static Map<String, Object> map = new HashMap<>();

    public static void main(String[] args) {

        map.put("name", "blade");
        map.put("jdk", 1.8);

        System.out.println(JsonKit.toString(map));

        Blade.me()
                .get("/json", ((request, response) -> response.json(map)))
                .showFileList(true)
                .listen(9001)
                .start(Application.class);
    }

}
