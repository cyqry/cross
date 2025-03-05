package com.ytycc.strategy.keylock;

import ch.qos.logback.core.util.TimeUtil;
import com.google.common.util.concurrent.Striped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;

public class StripedLockStrategy implements KeyLockStrategy {
    private static final Logger log = LoggerFactory.getLogger(StripedLockStrategy.class);


    private final Striped<Lock> stripes = Striped.lock(32);

    public StripedLockStrategy() {
    }

    @Override
    public <T> T executeSafelyWithKey(Object key, Supplier<T> function) {
        Lock lock;
        synchronized (this) {
            lock = stripes.get(key);
        }
        lock.lock();
        try {
            return function.get();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void deleteKey(Object key) {
        Thread timer = new Thread(() -> {
            try {
                TimeUnit.SECONDS.sleep(30);
                log.error("死锁");
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
        timer.start();
        synchronized (this) {
            Lock lock = stripes.get(key);
            try {
                while (!lock.tryLock()) {
                    Thread.onSpinWait();
                }
                //remove
            } finally {
                lock.unlock();
            }
        }
        timer.interrupt();

    }
}
