package com.ytycc.dispatch.tertiumquid.pool;


import com.ytycc.dispatch.tertiumquid.ForwardChannel;
import com.ytycc.utils.ConnectUtil;
import io.netty.channel.Channel;
import org.springframework.lang.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static com.ytycc.log.RunLog.ERROR_LOGGER;

public class ForwardChannelServerPool {
    public final List<ForwardChannel> resourceList = new ArrayList<>();

    private int next = -1;

    //存活连接数探测
    public void aliveDetection() {
        new Thread(() -> {
            while (true) {
                try {
                    TimeUnit.SECONDS.sleep(5);
                } catch (InterruptedException ignored) {
                }
                clearUnActive();
                System.out.println("剩余" + resourceList.size() + "存活的连接");
            }
        }).start();
    }

    public synchronized void clearUnActive() {
        resourceList.removeIf(forwardChannel -> {
            Channel channel = forwardChannel.channel();
            //清理不活跃的连接
            if (ConnectUtil.isInActive(channel)) {
                channel.close();
                return true;
            } else {
                return false;
            }

        });
    }


    public synchronized void register(@NonNull ForwardChannel forwardChannel) {
        if (resourceList.contains(forwardChannel)) {
            ERROR_LOGGER.error("重复的转发连接");
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
