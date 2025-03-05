package com.ytycc.dispatch.handler.receive;

import com.ytycc.dispatch.message.Protocol;
import com.ytycc.utils.ConnectUtil;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;

import static com.ytycc.constant.Const.AUTH_KEY;

public class HeartbeatHandler extends ChannelDuplexHandler {
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {

        //判断Authenticated, 校验后再ping
        if (ctx.channel().hasAttr(AUTH_KEY)) {
            if (evt instanceof IdleStateEvent e) {
                if (e.state() == IdleState.READER_IDLE) {
                    ctx.close();
                } else if (e.state() == IdleState.WRITER_IDLE) {
                    if (ConnectUtil.isWriteable(ctx.channel())) {
                        ctx.writeAndFlush(Protocol.ping());
                    }
                }
            }
        }

        super.userEventTriggered(ctx, evt);
    }
}
