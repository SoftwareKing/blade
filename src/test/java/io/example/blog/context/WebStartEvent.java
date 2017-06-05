package io.example.blog.context;

import com.blade.ioc.annotation.Bean;
import com.blade.event.Event;
import com.blade.event.StartedEvent;

/**
 * @author biezhi
 *         2017/6/1
 */
@Bean
public class WebStartEvent implements StartedEvent {

    @Override
    public void handleEvent(Event e) {
        System.out.println("启动后执行...");
    }
}
