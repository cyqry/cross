package com.ytycc.strategy.processing;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.ytycc.dispatch.Subscriber;
import io.netty.channel.nio.NioEventLoopGroup;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class ParallelConsumptionStrategy<T> implements EventConsumptionStrategy<T> {

    private final ThreadPoolExecutor executor;

    public ParallelConsumptionStrategy() {
        int coreCount = Runtime.getRuntime().availableProcessors();
        int maxSize = coreCount * 2;

        ThreadFactory threadFactory = new ThreadFactoryBuilder()
                .setNameFormat("Consumption-Thread-%d")
                .setDaemon(false)
                .setPriority(Thread.NORM_PRIORITY)
                .build();

        this.executor = new ThreadPoolExecutor(
                coreCount,
                maxSize,
                60,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(),
                threadFactory,
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }

    @Override
    public void process(T event, Consumer<T> consumer) {
        executor.submit(() -> consumer.accept(event));
    }


}
