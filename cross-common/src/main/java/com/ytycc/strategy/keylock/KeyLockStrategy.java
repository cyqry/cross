package com.ytycc.strategy.keylock;

import java.util.function.Function;
import java.util.function.Supplier;




/**
 * 对key加锁的策略
 * todo
 */
public interface KeyLockStrategy {
    <T> T executeSafelyWithKey(Object key, Supplier<T> function);

    void deleteKey(Object key);
}
