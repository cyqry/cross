package com.ytycc.dispatch.tertiumquid;

import com.ytycc.annotations.ByteBufAction;
import com.ytycc.annotations.ByteBufHandling;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelPromise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;


public abstract class AbstractCommunicable implements Communicable, Release {

    public static final Logger LOGGER = LoggerFactory.getLogger(AbstractCommunicable.class);

    //Close帧和连接关闭或异常 导致 release
    private boolean released = false;

    private int next = 0;


    private final Map<Integer, ChannelEvent> eventMap = new HashMap<>();


    @Override
    public synchronized ChannelFuture writeByOrder(int order, @ByteBufHandling(ByteBufAction.RELEASE) ByteBuf buf) {
        if (released) {
            buf.release();
            ChannelPromise promise = newPromise();
            promise.setFailure(new IllegalStateException("id:" + longId() + ",写入时channel已经释放"));
            return promise;
        }

        return run(ChannelEvent.writeEvent(order, buf));
    }

    @Override
    public synchronized ChannelFuture closeByOrder(int order) {
        if (released) {
            ChannelPromise promise = newPromise();
            promise.setFailure(new IllegalStateException("id:" + longId() + ",关闭时channel已经释放"));
            return promise;
        }
        return run(ChannelEvent.closeEvent(order));
    }

    @Override
    public synchronized void release() {
        if (!eventMap.isEmpty()) {
            LOGGER.info("id:{},next:{},eventMap 清理时不为空:{}", longId(), next, mapFormat());
        }
        eventMap.forEach((k, v) -> v.release());
        eventMap.clear();
        this.released = true;
    }

    //保证同一channel上的事件按序号顺序执行
    private ChannelFuture run(ChannelEvent channelEvent) {
        ChannelPromise promise = newPromise();
        int order = channelEvent.msgOrder();
        if (order == next) {
            eventMap.remove(order);
            if (channelEvent.isClose()) {
                ChannelFuture close = close();
                //检查逻辑
                if (!eventMap.isEmpty()) {
                    LOGGER.error("关闭事件执行，不应该还有其他缓存事件,id:{},next:{},{}", longId(), next, mapFormat());
                }
                release();
                return close;
            } else if (channelEvent.isWrite()) {
                ChannelFuture writeFuture = writeAndFlush(channelEvent.takeBuf());
                try {
                    writeFuture.sync();
                } catch (InterruptedException e) {
                    promise.setFailure(e);
                    return promise;
                }
                if (writeFuture.isSuccess()) {
                    int n = ++next;
                    if (eventMap.containsKey(n)) {
                        ChannelFuture future;
                        try {
                            future = run(eventMap.get(n)).sync();
                        } catch (InterruptedException e) {
                            promise.setFailure(e);
                            return promise;
                        }
                        if (future.isSuccess()) {
                            promise.setSuccess();
                        } else {
                            promise.setFailure(future.cause());
                        }

                    } else {
                        promise.setSuccess();
                    }
                } else {
                    promise.setFailure(writeFuture.cause());
                }
            } else {
                throw new IllegalStateException("Unreachable!");
            }

        } else if (order > next) {
            eventMap.put(order, channelEvent);
            promise.setSuccess();
        } else {
            channelEvent.release();
            LOGGER.error("已经执行过的事件,order:{},next:{},event：{}", order, next, channelEvent);
            promise.setFailure(new RuntimeException("已经执行过的事件"));
        }
        return promise;
    }


    private String mapFormat() {
        StringBuilder result = new StringBuilder();

        if (eventMap.isEmpty()) {
            result.append("map is empty");
        } else {
            eventMap.forEach((key, value) -> {
                result.append(String.format("Event with Order %d: %s%n", key, value));
            });
        }

        return result.toString();
    }

    abstract ChannelFuture writeAndFlush(ByteBuf byteBuf);

    abstract ChannelPromise newPromise();

    abstract ChannelFuture close();
}
