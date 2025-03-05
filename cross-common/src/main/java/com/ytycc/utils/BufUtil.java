package com.ytycc.utils;

import com.ytycc.annotations.ByteBufAction;
import com.ytycc.annotations.ByteBufHandling;
import io.netty.buffer.ByteBuf;

public class BufUtil {
    public static ByteBuf copy(@ByteBufHandling(ByteBufAction.KEEP) ByteBuf buf) {
        return buf.copy();
    }
}
