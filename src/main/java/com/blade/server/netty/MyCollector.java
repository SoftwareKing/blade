package com.blade.server.netty;

import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

/**
 * @author biezhi
 *         2017/6/2
 */
public class MyCollector<R> implements Collector<Class, Object, R> {


    @Override
    public Supplier<Object> supplier() {
        return null;
    }

    @Override
    public BiConsumer<Object, Class> accumulator() {
        return null;
    }

    @Override
    public BinaryOperator<Object> combiner() {
        return null;
    }

    @Override
    public Function<Object, R> finisher() {
        return null;
    }

    @Override
    public Set<Characteristics> characteristics() {
        return null;
    }
}
