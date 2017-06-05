package com.blade.event;

/**
 * web server start event
 *
 * @author biezhi
 *         2017/6/1
 */
@FunctionalInterface
public interface StartedEvent extends EventListener {

    void handleEvent(Event e);

}
