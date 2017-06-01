package com.blade.banner;

import com.blade.Blade;

public class BladeBanner implements Banner {

    static final String[] banner = {
            " __, _,   _, __, __,",
            " |_) |   /_\\ | \\ |_",
            " |_) | , | | |_/ |",
            " ~   ~~~ ~ ~ ~   ~~~"
    };

    @Override
    public String startText() {
        StringBuffer text = new StringBuffer();
        for (String s : banner) {
            text.append("\r\n\t\t" + s);
        }
        text.append("\r\n\t\t :: Blade :: (v" + Blade.VER + ")\r\n");
        return text.toString();
    }
}