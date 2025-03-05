package com.ytycc.dispatch;

public interface Subscriber<T> {
    void handleEvent(T event);
}
