package com.ytycc.dispatch;

import io.netty.channel.ChannelFuture;

//与服务器通信连接的生产者
public interface ForwardChannelProducer {

    ChannelFuture connectForwardServer();
}
