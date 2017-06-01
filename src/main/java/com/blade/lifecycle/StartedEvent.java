package com.blade.lifecycle;

/**
 * @author biezhi
 *         2017/6/1
 */
@FunctionalInterface
public interface StartedEvent extends EventListener {

    void handleEvent(Event e);

}
