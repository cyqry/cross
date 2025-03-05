package com.ytycc.strategy.keylock;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;

public class SynchronizedStrategy implements KeyLockStrategy {



    //为相同的id实现同步锁
    private static final List<Object> keys = new ArrayList<>();

    //当内容相同时，返回同一个对象
    private synchronized Object internCache(Object s) {
        int index = keys.indexOf(s);
        if (index != -1) {
            return keys.get(index);
        } else {
            keys.add(s);
            return s;
        }
    }

    //todo 对于每一个key，应保证锁正在使用时等待，等使用完再删除
    private synchronized void removeCache(Object s) {
        keys.remove(s);
    }

    @Override
    public <T> T executeSafelyWithKey(Object key, Supplier<T> function) {
        Object keyRef = internCache(key);
        synchronized (keyRef) {
            return function.get();
        }
    }

    @Override
    public void deleteKey(Object key) {
        removeCache(key);
    }
}
