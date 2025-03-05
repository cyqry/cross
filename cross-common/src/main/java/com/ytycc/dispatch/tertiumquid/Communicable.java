package com.ytycc.dispatch.tertiumquid;

import com.ytycc.annotations.ByteBufAction;
import com.ytycc.annotations.ByteBufHandling;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;

public interface Communicable {

    String longId();

    ChannelFuture writeByOrder(int order, @ByteBufHandling(ByteBufAction.RELEASE) ByteBuf buf);

    ChannelFuture closeByOrder(int order);
}
