package com.ytycc.dispatch.handler.forward;

import com.ytycc.constant.DefaultConst;
import com.ytycc.dispatch.message.Protocol;
import com.ytycc.dispatch.tertiumquid.ForwardChannel;
import com.ytycc.dispatch.tertiumquid.pool.ForwardChannelServerPool;
import com.ytycc.manager.IpStateManager;
import com.ytycc.service.UserService;
import com.ytycc.utils.ChannelUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.util.AttributeKey;


import static com.ytycc.log.RunLog.ERROR_LOGGER;

public class ForwardInitialAuthHandler extends ChannelInboundHandlerAdapter {

    private static final AttributeKey<String> AUTH_ATTR = AttributeKey.valueOf(DefaultConst.Authenticated);

    private final ForwardChannelServerPool forwardChannelServerPool;

    private final UserService userService;

    private final IpStateManager ipStateManager;

    public ForwardInitialAuthHandler(ForwardChannelServerPool forwardChannelServerPool, UserService userService, IpStateManager ipStateManager) {
        this.forwardChannelServerPool = forwardChannelServerPool;
        this.userService = userService;
        this.ipStateManager = ipStateManager;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        Channel readChannel = ctx.channel();
        ByteBuf buf = (ByteBuf) msg;
        String clientIp = ChannelUtil.takeIp(readChannel);
        if (!readChannel.hasAttr(AUTH_ATTR)) {
            IpStateManager.IpState state = ipStateManager.getIpState(clientIp);
            if (!userService.authenticate(readChannel, buf)) {
                state.incrementErrorCount();
                readChannel.writeAndFlush(Protocol.auth(false))
                        .addListener(ChannelFutureListener.CLOSE);
                ERROR_LOGGER.info("连接校验错误!");
                buf.release();
                return;
            }
            state.resetErrorCount();

            initializeAfterAuth(ctx);
            buf.release();
            return;
        }

        super.channelRead(ctx, msg);
    }


    private void initializeAfterAuth(ChannelHandlerContext ctx) {
        ctx.channel().attr(AUTH_ATTR);
        //通知校验成功
        ctx.channel().writeAndFlush(Protocol.auth(true)).addListener(
                (ChannelFutureListener) future -> {
                    if (future.isSuccess()) {
                        forwardChannelServerPool.register(new ForwardChannel(ctx.channel()));
                    } else {
                        future.channel().close();
                    }
                }
        );

    }
}
