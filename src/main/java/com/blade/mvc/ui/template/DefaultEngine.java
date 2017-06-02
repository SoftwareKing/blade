package com.blade.mvc.ui.template;

import com.blade.BladeException;
import com.blade.kit.BladeKit;
import com.blade.mvc.ui.ModelAndView;
import com.blade.server.netty.BladeServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.Writer;

/**
 * @author biezhi
 *         2017/5/31
 */
public class DefaultEngine implements TemplateEngine {

    private static final Logger log = LoggerFactory.getLogger(DefaultEngine.class);

    @Override
    public void render(ModelAndView modelAndView, Writer writer) {
        String view = modelAndView.getView();
        String viewPath = BladeServer.CLASSPATH + "templates" + File.separator + view;
        try {
            String body = BladeKit.readToString(viewPath);
            writer.write(body);
        } catch (Exception e) {
            throw new BladeException(e.getMessage());
        } finally {
            BladeKit.closeQuietly(writer);
        }
    }
}
