package com.blade.mvc.ui.template;

import com.blade.BladeException;
import com.blade.kit.IOKit;
import com.blade.mvc.WebContext;
import com.blade.mvc.http.Request;
import com.blade.mvc.ui.ModelAndView;
import com.blade.server.netty.WebServer;

import java.io.File;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

/**
 * default template implment
 *
 * @author biezhi
 *         2017/5/31
 */
public class DefaultEngine implements TemplateEngine {

    @Override
    public void render(ModelAndView modelAndView, Writer writer) {
        String view = modelAndView.getView();
        String viewPath = WebServer.CLASSPATH + "templates" + File.separator + view;
        try {
            Request request = WebContext.request();
            String body = IOKit.readToString(viewPath);

            Map<String, Object> attrs = new HashMap<>();
            attrs.putAll(request.attributes());
            attrs.putAll(request.session().attributes());

            String result = BladeTemplate.template(body, attrs).fmt();
            writer.write(result);
        } catch (Exception e) {
            throw new BladeException(e);
        } finally {
            IOKit.closeQuietly(writer);
        }
    }
}
