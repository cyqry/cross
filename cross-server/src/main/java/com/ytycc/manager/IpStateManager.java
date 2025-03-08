package com.ytycc.manager;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.time.Duration;
import java.util.Deque;
import java.util.LinkedList;
import java.util.concurrent.TimeUnit;

public class IpStateManager {

    private final Cache<String, IpState> ipCache;
    private final int maxAttempts;
    private final long windowMillis;


    public IpStateManager(int windowDuration, TimeUnit windowUnit, int maxAttempts) {
        this.maxAttempts = maxAttempts;
        this.windowMillis = windowUnit.toMillis(windowDuration);
        this.ipCache = Caffeine.newBuilder()
                // 在最后一次访问后的窗口时间后过期，自动清理内存
                .expireAfterAccess(Duration.ofMillis(windowMillis * 2))
                .build();
    }


    public void recordError(String ip) {
        IpState ipState = ipCache.get(ip, k -> new IpState());

        //实现滑动窗口算法
        synchronized (ipState) {
            long now = System.currentTimeMillis();
            long cutoff = now - windowMillis;
            Deque<Long> errorTimestamps = ipState.errorTimestamps;
            errorTimestamps.addLast(now);
            while (!errorTimestamps.isEmpty() && errorTimestamps.getFirst() < cutoff) {
                errorTimestamps.removeFirst();
            }
        }
    }

    public IpState getIpState(String ip) {
        return ipCache.get(ip, k -> new IpState());
    }

    public void clearIp(String ip) {
        ipCache.invalidate(ip);
    }

    public class IpState {
        private final Deque<Long> errorTimestamps;

        private IpState() {
            errorTimestamps = new LinkedList<>();
        }


        public synchronized boolean hasReachedErrorMaxCount() {
            return errorTimestamps.size() <= maxAttempts;
        }
    }
}