package com.ytycc.dispatch.tertiumquid;

import com.ytycc.annotations.ByteBufAction;
import com.ytycc.annotations.ByteBufHandling;
import io.netty.buffer.ByteBuf;

public class ForwardEvent {
    private String channelId;

    private byte type;
    private int msgOrder;
    private volatile ByteBuf buf;


    private ForwardEvent() {
    }

    public static ForwardEvent messageEvent(String id, int msgOrder, @ByteBufHandling(ByteBufAction.RELEASE) ByteBuf buf) {
        ForwardEvent event = new ForwardEvent();
        event.type = 1;
        event.channelId = id;
        event.msgOrder = msgOrder;
        event.buf = buf;
        return event;
    }

    public static ForwardEvent closeEvent(String id, int msgOrder) {
        ForwardEvent event = new ForwardEvent();
        event.type = 0;
        event.channelId = id;
        event.msgOrder = msgOrder;
        return event;
    }

    public String channelId() {
        return channelId;
    }

    public int msgOrder() {
        return msgOrder;
    }

    public ByteBuf takeBuf() {
        ByteBuf buf0 = buf;
        buf = null;
        return buf0;
    }

    public void release() {
        if (buf != null) {
            buf.release();
        }
    }

    public boolean isClose() {
        return type == 0;
    }

    public boolean isMessage() {
        return type == 1;
    }
}
