package com.blade.mvc.ui;

import com.blade.Blade;

/**
 * @author biezhi
 *         2017/6/2
 */
public class DefaultUI {

    public static final String HTML = "<!DOCTYPE html><html><head><meta charset='utf-8'><title>500 Internal Error</title>"
            + "<style type='text/css'>*{margin:0;padding:0;font-weight:400;}.info{margin:0;padding:10px;color:#000;background-color:#fff;height:60px;line-height:60px;border-bottom:5px solid #15557a}.isa_error{margin:0;padding:10px;font-size:14px;font-weight:bold;background-color:#e9eff1;border-bottom:1px solid #000}.version{padding:10px;text-decoration-line: none;}</style></head><body>"
            + "<div class='info'><h3>%s</h3></div><div class='isa_error'><pre>";

    public static final String END = "</pre></div><p class='version'><a href='https://github.com/biezhi/blade' target='_blank'>Blade-" + Blade.VER + "</a><p></body></html>";

    /**
     * server 500
     */
    public static final String VIEW_500 = "<html><head><title>500 Internal Error</title></head><body bgcolor=\"white\"><center><h1>500 Internal Error</h1></center><hr><center>blade " + Blade.VER + "</center></body></html>";

    /**
     * server 404
     */
    public static final String VIEW_404 = "<html><head><title>404 Not Found</title></head><body bgcolor=\"white\"><center><h1>[ %s ] Not Found</h1></center><hr><center>blade " + Blade.VER + "</center></body></html>";

    /**
     * server 405
     */
    public static final String VIEW_405 = "<html><head><title>403 Uri Forbidden</title></head><body bgcolor=\"white\"><center><h1>[ %s ] Method Not Allowed</h1></center><hr><center>blade " + Blade.VER + "</center></body></html>";

}
