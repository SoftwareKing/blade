package com.blade.lifecycle;

import com.blade.Blade;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class EventManager {

    private Map<Event.Type, List<EventListener>> listenerMap;

    public EventManager() {
        this.listenerMap = Stream.of(Event.Type.values()).collect(Collectors.toMap(v -> v, v -> new LinkedList<>()));
    }

    public void addEventListener(Event.Type type, EventListener listener) {
        listenerMap.get(type).add(listener);
    }

    public void fireEvent(Event.Type type, Blade blade) {
        listenerMap.get(type).forEach(listener -> listener.handleEvent(new Event(type, blade)));
    }

    public void fireEvent(Event.Type type) {
        fireEvent(type, null);
    }

}