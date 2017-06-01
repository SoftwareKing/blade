package com.example.blog;

import com.blade.Blade;

/**
 * @author biezhi
 *         2017/5/31
 */
public class Application {

    public static void main(String[] args) {
        Blade.me()
                .showFileList(true)
                .listen(9001)
                .start(Application.class);
    }

}
