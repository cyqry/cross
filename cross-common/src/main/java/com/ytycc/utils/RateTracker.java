package com.ytycc.utils;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class RateTracker {
    private final Counter counter;
    private final ScheduledExecutorService scheduler;
    private volatile double previousCount = 0;

    private final List<Double> rateHistory = new ArrayList<>();
    private double maxRate = 0; // 历史最大速率

    /**
     * @param meterRegistry           MeterRegistry.
     * @param metricName              要跟踪的Tracker的名称.
     * @param reportIntervalInSeconds 报告速率的时间间隔（以秒为单位）.
     */
    public RateTracker(MeterRegistry meterRegistry, String metricName, int reportIntervalInSeconds) {
        this.counter = Counter.builder(metricName)
                .description("Tracks the invocation rate of a method")
                .register(meterRegistry);

        this.scheduler = Executors.newSingleThreadScheduledExecutor();
        this.scheduler.scheduleAtFixedRate(this::reportStatistics, 0, reportIntervalInSeconds, TimeUnit.SECONDS);
    }

    public void record() {
        counter.increment();
    }

    private void reportStatistics() {
        double currentCount = counter.count();
        double rate = currentCount - previousCount; // 当前时间段的速率
        previousCount = currentCount;

        if (rate > maxRate) {
            maxRate = rate;
        }

        if (rate <= 0) {
            return;
        }

        rateHistory.add(rate);
        double average = calculateAverage();
        double median = calculateMedian();

        System.out.printf("[%s] 当前速率: %.2f 次/秒, 历史最大速率: %.2f 次/秒, 非零值平均速率: %.2f 次/秒, 中位数速率: %.2f 次/秒，总次数: %.2f次%n",
                counter.getId().getName(), rate, maxRate, average, median, currentCount);
    }

    /**
     * 平均数
     */
    private double calculateAverage() {
        if (rateHistory.isEmpty()) {
            return 0;
        }
        return rateHistory.stream().mapToDouble(Double::doubleValue).average().orElse(0);
    }

    /**
     * 中位数
     */
    private double calculateMedian() {
        if (rateHistory.isEmpty()) {
            return 0;
        }
        List<Double> sortedRates = new ArrayList<>(rateHistory);
        Collections.sort(sortedRates);
        int size = sortedRates.size();
        if (size % 2 == 0) {
            return (sortedRates.get(size / 2 - 1) + sortedRates.get(size / 2)) / 2.0;
        } else {
            return sortedRates.get(size / 2);
        }
    }

    /**
     * 关闭报告定时任务.
     */
    public void shutdown() {
        scheduler.shutdown();
    }
}
