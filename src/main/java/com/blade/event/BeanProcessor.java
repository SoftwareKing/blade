package com.blade.event;

import com.blade.Blade;

@FunctionalInterface
public interface BeanProcessor {

    default void prev(Blade blade) {
    }

    void processor(Blade blade);

}