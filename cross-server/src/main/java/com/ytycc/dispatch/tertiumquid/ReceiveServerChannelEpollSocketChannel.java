package com.ytycc.dispatch.tertiumquid;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelPromise;
import io.netty.channel.epoll.EpollSocketChannel;

public class ReceiveServerChannelEpollSocketChannel extends AbstractCommunicable implements ReceiveServerChannelEnhancer {
    private final EpollSocketChannel delegate;

    String id;

    public ReceiveServerChannelEpollSocketChannel(EpollSocketChannel delegate) {
        this.delegate = delegate;
        this.id = delegate.id().asLongText();
    }

    @Override
    public String longId() {
        return id;
    }

    @Override
    public  ChannelFuture writeByOrder(int order, ByteBuf buf) {
        ChannelPromise promise = newPromise();
        delegate.eventLoop().submit(() -> {
            ChannelFuture future = super.writeByOrder(order, buf);
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
    public  ChannelFuture closeByOrder(int order) {
        ChannelPromise promise = newPromise();
        delegate.eventLoop().submit(() -> {
            ChannelFuture future = super.closeByOrder(order);
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
    ChannelFuture writeAndFlush(ByteBuf byteBuf) {
        return delegate.writeAndFlush(byteBuf);
    }

    @Override
    ChannelPromise newPromise() {
        return delegate.newPromise();
    }

    @Override
    ChannelFuture close() {
        return delegate.close();
    }


}
