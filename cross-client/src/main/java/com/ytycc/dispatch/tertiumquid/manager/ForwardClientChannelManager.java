package com.ytycc.dispatch.tertiumquid.manager;

import com.ytycc.annotations.ByteBufAction;
import com.ytycc.annotations.ByteBufHandling;
import com.ytycc.dispatch.tertiumquid.ForwardClientChannel;
import com.ytycc.dispatch.tertiumquid.ForwardClientNioSocketChannel;
import com.ytycc.utils.BufUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.util.internal.StringUtil;
import org.junit.Assert;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static com.ytycc.constant.Const.CLEAR_KEY;

public class ForwardClientChannelManager {
    private final ConcurrentHashMap<String, ForwardClientChannel> map = new ConcurrentHashMap<>();

    public boolean put(String uuid, ForwardClientChannel channel) {
        Assert.assertFalse("uuid must not be empty", StringUtil.isNullOrEmpty(uuid));
        Assert.assertFalse("channel must not be null", Objects.isNull(channel));
        return map.putIfAbsent(uuid, channel) == null;
    }


    public Optional<ForwardClientChannel> getForwardClientChannel(String uuid) {
        return Optional.ofNullable(map.get(uuid));
    }


    public Optional<ForwardClientChannel> remove(String uuid) {
        return Optional.ofNullable(map.remove(uuid));
    }

    public boolean containsID(String id) {
        return map.containsKey(id);
    }

    public boolean containsChannel(ForwardClientChannel channel) {
        return map.containsValue(channel);
    }

    public void clear() {
        map.forEach((k, v) -> {
            v.attr(CLEAR_KEY).set(true);
            v.close();
        });
        map.clear();
    }

}
