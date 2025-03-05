package com.ytycc.dispatch.tertiumquid;

import com.ytycc.annotations.ByteBufAction;
import com.ytycc.annotations.ByteBufHandling;
import com.ytycc.utils.BufUtil;
import io.netty.buffer.ByteBuf;

public class ChannelEvent implements Release {
    private int msgOrder;

    private int type;
    private ByteBuf buf;

    private ChannelEvent() {
    }

    @Override
    public void release() {
        if (buf != null) {
            buf.release();
            buf = null;
        }
    }

    public static ChannelEvent closeEvent(int msgOrder) {
        ChannelEvent channelEvent = new ChannelEvent();
        channelEvent.msgOrder = msgOrder;
        channelEvent.type = 0;
        return channelEvent;
    }

    public ByteBuf takeBuf() {
        if (type == 0) {
            throw new IllegalArgumentException("close事件没有buf");
        }
        ByteBuf buf = this.buf;
        this.buf = null;
        return buf;
    }

    public byte[] copyBuf() {
        if (this.buf == null) {
            throw new RuntimeException("该event已被释放");
        }
        byte[] bytes = new byte[this.buf.readableBytes()];
        this.buf.getBytes(0, bytes);
        return bytes;
    }

    public static ChannelEvent writeEvent(int msgOrder, @ByteBufHandling(ByteBufAction.RELEASE) ByteBuf buf) {
        ChannelEvent channelEvent = new ChannelEvent();
        channelEvent.buf = buf;
        channelEvent.msgOrder = msgOrder;
        channelEvent.type = 1;
        return channelEvent;
    }

    public int msgOrder() {
        return this.msgOrder;
    }

    public boolean isWrite() {
        return type == 1;
    }

    public boolean isClose() {
        return type == 0;
    }


    @Override
    public String toString() {
        return "ChannelEvent{" +
                "msgOrder=" + msgOrder +
                ", type=" + type +
                ", buf=" + (buf != null ? "ByteBuf(" + buf.readableBytes() + " bytes)" : "null") +
                '}';
    }
}
