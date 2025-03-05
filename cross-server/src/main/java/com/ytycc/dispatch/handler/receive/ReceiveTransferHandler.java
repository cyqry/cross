package com.ytycc.dispatch.handler.receive;

import com.ytycc.annotations.ByteBufAction;
import com.ytycc.annotations.ByteBufHandling;
import com.ytycc.dispatch.message.Frame;
import com.ytycc.dispatch.message.FrameCode;
import com.ytycc.dispatch.message.Protocol;
import com.ytycc.dispatch.tertiumquid.ForwardChannel;
import com.ytycc.dispatch.tertiumquid.ReceiveServerChannelEnhancer;
import com.ytycc.dispatch.tertiumquid.ReceiveServerChannelEpollSocketChannel;
import com.ytycc.dispatch.tertiumquid.manager.ReceiveServerChannelManager;
import com.ytycc.dispatch.tertiumquid.pool.ForwardChannelServerPool;
import com.ytycc.utils.BufUtil;
import com.ytycc.utils.ChannelUtil;
import com.ytycc.utils.RateTracker;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;

import java.util.Optional;


import static com.ytycc.constant.AttrConst.EXTEND_KEY;
import static com.ytycc.constant.AttrConst.NO_NOTIFY_KEY;
import static com.ytycc.log.RunLog.*;

public class ReceiveTransferHandler extends SimpleChannelInboundHandler<ByteBuf> {

    private static final AttributeKey<Integer> ORDER_KEY = AttributeKey.valueOf("orderKey");

    private final ForwardChannelServerPool forwardChannelServerPool;
    private final ReceiveServerChannelManager receiveServerChannelManager;

    private static final RateTracker rateTracker = new RateTracker(new SimpleMeterRegistry(), "outside_channel_active", 1);


    public ReceiveTransferHandler(ForwardChannelServerPool forwardChannelServerPool, ReceiveServerChannelManager receiveServerChannelManager) {
        this.forwardChannelServerPool = forwardChannelServerPool;
        this.receiveServerChannelManager = receiveServerChannelManager;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        rateTracker.record();
        Channel channel = ctx.channel();
        try {
            if (forwardChannelServerPool.isEmpty()) {
                System.out.println("暂无客户端，拒绝外界连接");
                ctx.channel().attr(NO_NOTIFY_KEY).set(true);
                ctx.close();
                return;
            }
            Optional<ForwardChannel> resource = forwardChannelServerPool.findResource();
            if (resource.isEmpty()) {
                System.out.println("客户端繁忙，拒绝连接");
                ctx.channel().attr(NO_NOTIFY_KEY).set(true);
                ctx.close();
                return;
            }

            assignReceiveServerChannel(ctx);
            ReceiveServerChannelEnhancer enhancer = ChannelUtil.resolveReceiveServerChannel(ctx.channel());
            if (receiveServerChannelManager.put(enhancer.longId(), channel)) {
                resource.get().writeOpenFrame(enhancer.longId());
                OPEN_LOGGER.info("channels.size = {}", receiveServerChannelManager.size());
            } else {
                ERROR_LOGGER.error("id:{} or channel:{} already exists.", enhancer.longId(), channel);
                ctx.close();
            }
        } finally {
            super.channelActive(ctx);
        }

    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        ReceiveServerChannelEnhancer enhancer = ChannelUtil.resolveReceiveServerChannel(ctx.channel());
        receiveServerChannelManager.remove(enhancer.longId());
        enhancer.release();
        CLOSE_LOGGER.info("与外界连接关闭, id : {}", enhancer.longId());
        //通知客户端清理
        if (!Boolean.TRUE.equals(ctx.channel().attr(NO_NOTIFY_KEY).get())) {
            Optional<ForwardChannel> resource = forwardChannelServerPool.findResource();
            if (resource.isPresent()) {
                resource.get().writeCloseFrame(enhancer.longId(), getChannelNextMsgOrder(ctx.channel())).addListener(f -> {
                    if (!f.isSuccess()) {
                        ERROR_LOGGER.error("向客户端发送关闭帧失败,id:{}", enhancer.longId(), f.cause());
                    }
                });
            } else {
                ERROR_LOGGER.error("客户端繁忙或者未连接，发送关闭帧失败,id:{}", enhancer.longId());
            }

        }

        super.channelInactive(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        ReceiveServerChannelEnhancer enhancer = ChannelUtil.resolveReceiveServerChannel(ctx.channel());
        ERROR_LOGGER.error("id:{},异常:", enhancer.longId(), cause);
        ctx.close();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, @ByteBufHandling(ByteBufAction.KEEP) ByteBuf buf) throws Exception {

        Channel channel = ctx.channel();
        ReceiveServerChannelEnhancer enhancer = ChannelUtil.resolveReceiveServerChannel(channel);
        Optional<ForwardChannel> resource = forwardChannelServerPool.findResource();
        int msgOrder = getChannelNextMsgOrder(channel);
        if (resource.isPresent()) {
            Frame frame = new Frame(FrameCode.MESSAGE, BufUtil.copy(buf));
            ByteBuf byteBuf = Protocol.transferFrameEncode(enhancer.longId(), msgOrder, frame);
            frame.release();
            resource.get().writeAndFlush(byteBuf).addListener(f -> {
                if (!f.isSuccess()) {
                    ERROR_LOGGER.error("向客户端发送消息失败,{}", enhancer.longId(), f.cause());
                }
            });
        } else {
            ERROR_LOGGER.info("无客户端或客户端繁忙,关闭与外界的连接:{}", channel);
            ctx.channel().attr(NO_NOTIFY_KEY).set(true);
            ctx.close();
        }

    }

    private int getChannelNextMsgOrder(Channel channel) {
        Attribute<Integer> orderAttribute = channel.attr(ORDER_KEY);
        int order;
        if (orderAttribute.get() == null) {
            order = 0;
        } else {
            order = orderAttribute.get() + 1;
        }
        orderAttribute.set(order);
        return order;
    }

    public void assignReceiveServerChannel(ChannelHandlerContext ctx) {
        Channel channel = ctx.channel();
        if (channel instanceof EpollSocketChannel epollChannel) {
            channel.attr(EXTEND_KEY).set(new ReceiveServerChannelEpollSocketChannel(epollChannel));
        }
    }

    private void test(ForwardChannel channel) {
        for (int i = 0; i < 10; i++) {
            ByteBuf test = Protocol.testFrame();
            channel.writeAndFlush(test);
        }
    }

}
