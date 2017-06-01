package com.blade.banner;

import java.io.IOException;

public final class BannerStarter {

    private static Banner BANNER = new BladeBanner();

    public static void printStart() {
        System.out.println(BANNER.startText());
    }

    public static void banner(Banner banner) {
        BANNER = banner;
    }

    public static void banner(final String text) {
        BANNER = () -> {
            try {
                return BannerFont.load().asAscii(text);
            } catch (IOException e) {
                return new BladeBanner().startText();
            }
        };
    }
}