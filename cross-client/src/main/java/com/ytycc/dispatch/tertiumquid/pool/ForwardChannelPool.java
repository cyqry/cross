package com.ytycc.dispatch.tertiumquid.pool;


import com.ytycc.dispatch.ForwardChannelProducer;
import com.ytycc.dispatch.tertiumquid.ForwardChannel;
import com.ytycc.utils.ConnectUtil;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;


public class ForwardChannelPool {

    private static final int MAX_CONNECTION_COUNT = 16;
    private static final Logger log = LoggerFactory.getLogger(ForwardChannelPool.class);

    private final ForwardChannelProducer forwardChannelProducer;
    private final List<ForwardChannel> resourceList = new ArrayList<>();
    private boolean detection = false;
    private int next = -1;


    //为重连次数计数
    public final AtomicInteger connectionCount = new AtomicInteger(0);


    public ForwardChannelPool(ForwardChannelProducer producer) {
        this.forwardChannelProducer = producer;
    }


    public void fill() {
        try {
            int n = MAX_CONNECTION_COUNT - resourceList.size();
            for (int i = 0; i < n; i++) {
                forwardChannelProducer.connectForwardServer();
            }
        } catch (Exception e) {
            log.error("连接异常", e);
        }
    }


    public void unAliveDetection() {
        synchronized (this) {
            if (!detection) {
                detection = true;
            } else {
                return;
            }
        }
        while (true) {
            try {
                TimeUnit.SECONDS.sleep(8);
                clearUnActive();
                System.out.println("剩余" + resourceList.size() + "存活的连接");
                if (connectionCount.get() > 500) {
                    System.out.println("重连次数过多");
                    System.exit(-1);
                }
                if (resourceList.size() < MAX_CONNECTION_COUNT * 3 / 4) {
                    for (int i = 0; i < MAX_CONNECTION_COUNT - resourceList.size(); i++) {
                        TimeUnit.SECONDS.sleep(1);
                        connectionCount.incrementAndGet();
                        System.out.println("尝试重连");
                        forwardChannelProducer.connectForwardServer().sync();
                    }
                }
                if (isEmpty()) {
                    System.out.println("服务端未开启");
                    System.exit(0);
                }

            } catch (InterruptedException e) {
                log.error("Interrupted",e);
            }

        }

    }


    public synchronized void clearUnActive() {
        resourceList.removeIf(forwardChannel -> {
            Channel channel = forwardChannel.channel();
            if (ConnectUtil.isInActive(channel)) {
                channel.close();
                return true;
            } else
                return false;

        });
    }


    public synchronized void register(ForwardChannel forwardChannel) {
        assert forwardChannel != null;
        if (resourceList.contains(forwardChannel)) {
            log.error("重复的forward连接加入池");
            return;
        }
        resourceList.add(forwardChannel);
    }

    public synchronized Optional<ForwardChannel> findResource() {
        if (resourceList.isEmpty()) {
            return Optional.empty();
        }

        if (resourceList.stream()
                .allMatch(f -> ConnectUtil.isUnWriteable(f.channel()))) {
            return Optional.empty();
        }


        int size = resourceList.size();

        for (int i = 0; i < size; i++) {
            next = (next + 1) % size;
            ForwardChannel resource = resourceList.get(next);
            if (ConnectUtil.isWriteable(resource.channel())) {
                return Optional.of(resource);
            }
        }
        return Optional.empty();
    }

    public synchronized boolean remove(Channel channel) {
        return resourceList.removeIf(channelResource -> channelResource.channel() == channel);
    }

    public synchronized boolean isEmpty() {
        return resourceList.isEmpty();
    }
}
