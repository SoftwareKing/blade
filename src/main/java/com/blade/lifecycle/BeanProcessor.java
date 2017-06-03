package com.blade.lifecycle;

import com.blade.Blade;

@FunctionalInterface
public interface BeanProcessor {

    default void prev(Blade blade) {
    }

    void processor(Blade blade);

}