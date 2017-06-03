package com.blade.mvc;

import com.blade.kit.StringKit;
import io.netty.handler.codec.http.HttpVersion;

/**
 * @author biezhi
 *         2017/6/2
 */
public interface Const {

    String CLASSPATH = Const.class.getResource("/").getPath();

    String CONTENT_TYPE_HTML = "text/html; charset=UTF-8";
    String CONTENT_TYPE_JSON = "application/json; charset=UTF-8";
    String CONTENT_TYPE_TEXT = "text/plain; charset=UTF-8";

    String HTTP_DATE_FORMAT = "EEE, dd MMM yyyy HH:mm:ss zzz";

    HttpVersion HTTP_VERSION = HttpVersion.HTTP_1_1;

    String WEB_JARS = "/webjars/";

    String PLUGIN_PACKAGE_NAME = "com.blade.plugin";

    String SESSION_COOKIE_NAME = "SESSION";

}
