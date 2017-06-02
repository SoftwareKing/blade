package com.blade;

import com.blade.kit.ason.Util;

import java.util.function.Consumer;

/**
 * @author biezhi
 *         2017/5/31
 */
public class BladeTest {

    public static void main(String[] args) {
        System.out.println(Util.splitPath("asdadadasd"));
        Blade blade = Blade.me();
        blade.get("/", (req, res) -> {
            res.text("Hello Blade");
        });

        blade.gzip(true).start();
    }

}