package com.ytycc.dispatch.tertiumquid;

import com.ytycc.annotations.ByteBufAction;
import com.ytycc.annotations.ByteBufHandling;
import com.ytycc.dispatch.message.Protocol;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;

import java.util.Objects;

/**
 * @param channel
 */
public record ForwardChannel(Channel channel) {


    public ChannelFuture writeAndFlush(@ByteBufHandling(ByteBufAction.RELEASE) ByteBuf buf) {
        return channel.writeAndFlush(buf);
    }

    public ChannelFuture writeForceCloseFrame(String id) {
        return channel.writeAndFlush(Protocol.forceCloseFrame(id));
    }

    public ChannelFuture writeCloseFrame(String id, int msgOrder) {
        return channel.writeAndFlush(Protocol.closeFrame(id, msgOrder));
    }

    public ChannelFuture writeOpenFrame(String id) {
        return channel.writeAndFlush(Protocol.openFrame(id));
    }

    public ChannelFuture writeOpenAckFrame(String id) {
        return channel.writeAndFlush(Protocol.openAckFrame(id));
    }

    public ChannelFuture writeCloseAckFrame(String id) {
        return channel.writeAndFlush(Protocol.closeAckFrame(id));
    }


    @Override
    public boolean equals(Object o) {
        return o instanceof ForwardChannel && Objects.equals(channel, ((ForwardChannel) o).channel);
    }

    @Override
    public int hashCode() {
        return Objects.hash(channel);
    }
}
