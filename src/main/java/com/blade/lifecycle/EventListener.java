package com.blade.lifecycle;

@FunctionalInterface
public interface EventListener {

    void handleEvent(Event e);

}