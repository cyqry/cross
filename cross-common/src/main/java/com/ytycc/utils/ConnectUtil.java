package com.ytycc.utils;

import io.netty.channel.Channel;


public class ConnectUtil {

    public static boolean isWriteable(Channel channel) {
        return channel.isActive() && channel.isWritable();
    }

    public static boolean isUnWriteable(Channel channel) {
        return !isWriteable(channel);
    }

    public static boolean isActive(Channel channel) {
        return channel.isActive();
    }

    public static boolean isInActive(Channel channel) {
        return !isActive(channel);
    }
}
