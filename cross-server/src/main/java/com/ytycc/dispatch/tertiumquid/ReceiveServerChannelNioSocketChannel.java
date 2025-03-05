package com.ytycc.dispatch.tertiumquid;

import com.ytycc.annotations.ByteBufAction;
import com.ytycc.annotations.ByteBufHandling;
import com.ytycc.strategy.processing.EventConsumptionStrategy;
import com.ytycc.strategy.processing.ParallelConsumptionStrategy;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelPromise;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.nio.channels.SocketChannel;


public class ReceiveServerChannelNioSocketChannel extends NioSocketChannel implements ReceiveServerChannelEnhancer {

    private final String longId;

    private final Proxy proxy;

    private static final EventConsumptionStrategy<Void> eventConsumptionStrategy = new ParallelConsumptionStrategy<>();

    public ReceiveServerChannelNioSocketChannel(Channel parent, SocketChannel socket) {
        super(parent, socket);
        this.proxy = new Proxy();
        this.longId = id().asLongText();
    }

    @Override
    public String longId() {
        return proxy.longId();
    }

    @Override
    public ChannelFuture writeByOrder(int order, @ByteBufHandling(ByteBufAction.RELEASE) ByteBuf buf) {
//        return proxy.writeByOrder(order, buf);
        ChannelPromise promise = newPromise();
        eventLoop().submit(() -> {
            ChannelFuture future = proxy.writeByOrder(order, buf);
            future.addListener(f -> {
                if (f.isSuccess()) {
                    promise.setSuccess();
                } else {
                    promise.setFailure(f.cause());
                }
            });
        });
        return promise;
    }

    @Override
    public ChannelFuture closeByOrder(int order) {
//        return proxy.closeByOrder(order);
        ChannelPromise promise = newPromise();
        eventLoop().submit(() -> {
            ChannelFuture future = proxy.closeByOrder(order);
            future.addListener(f -> {
                if (f.isSuccess()) {
                    promise.setSuccess();
                } else {
                    promise.setFailure(f.cause());
                }
            });
        });
        return promise;
    }

    @Override
    public void release() {
        proxy.release();
    }


    private class Proxy extends AbstractCommunicable {

        @Override
        ChannelPromise newPromise() {
            return ReceiveServerChannelNioSocketChannel.this.newPromise();
        }


        @Override
        ChannelFuture writeAndFlush(ByteBuf byteBuf) {
            return ReceiveServerChannelNioSocketChannel.this.writeAndFlush(byteBuf);
        }


        @Override
        ChannelFuture close() {
            return ReceiveServerChannelNioSocketChannel.this.close();
        }

        @Override
        public String longId() {
            return ReceiveServerChannelNioSocketChannel.this.longId;
        }
    }
}
