package com.ytycc.dispatch.handler.forward;

import com.ytycc.dispatch.message.Protocol;
import com.ytycc.utils.ConnectUtil;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleStateEvent;

public class HeartbeatHandler extends ChannelDuplexHandler {

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent idleEvent) {
            handleIdleEvent(ctx, idleEvent);
        } else {
            super.userEventTriggered(ctx, evt); // 确保其他事件的处理
        }
    }

    private void handleIdleEvent(ChannelHandlerContext ctx, IdleStateEvent idleEvent) {
        switch (idleEvent.state()) {
            case READER_IDLE -> handleReaderIdle(ctx);
            case WRITER_IDLE -> handleWriterIdle(ctx);
            default -> handleDefaultState(idleEvent);
        }
    }

    private void handleReaderIdle(ChannelHandlerContext ctx) {
        ctx.close(); // 关闭连接
    }

    private void handleWriterIdle(ChannelHandlerContext ctx) {
//        System.out.println("发送心跳包...");
        if (ConnectUtil.isWriteable(ctx.channel())) {
            ctx.writeAndFlush(Protocol.ping());
        }
    }

    private void handleDefaultState(IdleStateEvent idleEvent) {
    }
}
