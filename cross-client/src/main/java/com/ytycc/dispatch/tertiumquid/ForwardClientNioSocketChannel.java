package com.ytycc.dispatch.tertiumquid;

import com.ytycc.annotations.ByteBufAction;
import com.ytycc.annotations.ByteBufHandling;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelPromise;
import io.netty.channel.socket.nio.NioSocketChannel;

public class ForwardClientNioSocketChannel extends NioSocketChannel implements ForwardClientChannel {
    private String longId;

    private final ChannelInitializer channelInitializer = new ChannelInitializer();

    private final Proxy proxy = new Proxy();

    protected void setLongId(String longId) {
        this.longId = longId;
    }

    @Override
    public ChannelFuture reqByOrder(int order, @ByteBufHandling(ByteBufAction.RELEASE) ByteBuf buf) {
        return proxy.writeByOrder(order, buf);

    }

    @Override
    public ChannelFuture closeByOrder(int order) {
        if (order > -1) {
            return proxy.closeByOrder(order);
        } else {
            return close();
        }
    }

    public String longId() {
        return proxy.longId();
    }

    public void release() {
        proxy.release();
    }

    public ChannelInitializer channelInitializer() {
        return channelInitializer;
    }


    public class ChannelInitializer {
        ForwardClientNioSocketChannel channel = ForwardClientNioSocketChannel.this;

        public void init(String id) {
            if (channel.longId() != null) {
                throw new IllegalArgumentException("LongId is already init");
            }
            channel.setLongId(id);
        }
    }

    private class Proxy extends AbstractCommunicable {

        @Override
        ChannelFuture writeAndFlush(ByteBuf byteBuf) {
            return ForwardClientNioSocketChannel.this.writeAndFlush(byteBuf);
        }

        @Override
        ChannelPromise newPromise() {
            return ForwardClientNioSocketChannel.this.newPromise();
        }

        @Override
        ChannelFuture close() {
            return ForwardClientNioSocketChannel.this.close();
        }

        @Override
        public String longId() {
            return ForwardClientNioSocketChannel.this.longId;
        }
    }
}
