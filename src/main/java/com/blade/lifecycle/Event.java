package com.blade.lifecycle;

import com.blade.Blade;

public class Event {

    public enum Type {
        SERVER_STARTING,
        SERVER_STARTED,
        SERVER_STOPPING,
        SERVER_STOPPED
    }

    public Type eventType;
    public Blade blade;

    public Event(Type eventType) {
        this.eventType = eventType;
        this.blade = null;
    }

    public Event(Type eventType, Blade blade) {
        this.eventType = eventType;
        this.blade = blade;
    }

}