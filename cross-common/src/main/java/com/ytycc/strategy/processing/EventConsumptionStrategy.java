package com.ytycc.strategy.processing;

import com.ytycc.dispatch.Subscriber;

import java.util.function.Consumer;
import java.util.function.Predicate;

public interface EventConsumptionStrategy<T> {
    void process(T event, Consumer<T> consumer);
}
