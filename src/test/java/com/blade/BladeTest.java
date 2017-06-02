package com.blade;

import java.util.function.Consumer;

/**
 * @author biezhi
 *         2017/5/31
 */
public class BladeTest {

    public static void main(String[] args) {
        Blade blade = Blade.me();
        blade.get("/", (req, res) -> {
            res.text("Hello Blade");
        });

        blade.gzip(true).start();
    }

}