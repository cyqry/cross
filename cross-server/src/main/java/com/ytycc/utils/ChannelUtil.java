package com.ytycc.utils;

import com.ytycc.dispatch.tertiumquid.ReceiveServerChannelEnhancer;
import com.ytycc.dispatch.tertiumquid.ReceiveServerChannelEpollSocketChannel;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.util.AttributeKey;

import static com.ytycc.constant.AttrConst.CLIENT_IP_KEY;
import static com.ytycc.constant.AttrConst.EXTEND_KEY;


public class ChannelUtil {

    public static String takeIp(Channel channel) {
        return channel.attr(CLIENT_IP_KEY).get();
    }




    public static ReceiveServerChannelEnhancer resolveReceiveServerChannel(Channel channel) {
        if (channel instanceof ReceiveServerChannelEnhancer) {
            return (ReceiveServerChannelEnhancer) channel;
        } else {
            return channel.attr(EXTEND_KEY).get();
        }
    }
}
