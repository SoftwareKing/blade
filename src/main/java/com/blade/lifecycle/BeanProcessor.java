package com.blade.lifecycle;

import com.blade.Blade;

@FunctionalInterface
public interface BeanProcessor {

    void processor(Blade blade);

}