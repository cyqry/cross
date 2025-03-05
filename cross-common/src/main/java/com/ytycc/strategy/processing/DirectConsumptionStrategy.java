package com.ytycc.strategy.processing;

import com.ytycc.dispatch.Subscriber;

import java.util.function.Consumer;
import java.util.function.Predicate;

public class DirectConsumptionStrategy<T> implements EventConsumptionStrategy<T> {

    @Override
    public void process(T event, Consumer<T> consumer) {
        consumer.accept(event);
    }
}
