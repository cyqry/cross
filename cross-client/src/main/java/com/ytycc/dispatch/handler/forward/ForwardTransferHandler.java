package com.ytycc.dispatch.handler.forward;

import com.ytycc.annotations.ByteBufAction;
import com.ytycc.annotations.ByteBufHandling;
import com.ytycc.dispatch.Subscriber;
import com.ytycc.dispatch.tertiumquid.ForwardClientNioSocketChannel;
import com.ytycc.dispatch.tertiumquid.ForwardEvent;
import com.ytycc.utils.BufUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.ytycc.constant.Const.CLEAR_KEY;
import static com.ytycc.constant.Const.ID_KEY;

public class ForwardTransferHandler extends SimpleChannelInboundHandler<ByteBuf> {
    private static final AttributeKey<Integer> ORDER_KEY = AttributeKey.valueOf("orderKey");
    private static final Logger log = LoggerFactory.getLogger(ForwardTransferHandler.class);

    private final Subscriber<ForwardEvent> subscriber;

    public ForwardTransferHandler(Subscriber<ForwardEvent> subscriber) {
        this.subscriber = subscriber;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {

        ForwardClientNioSocketChannel channel = (ForwardClientNioSocketChannel) ctx.channel();
        String id = pollId(channel);
        channel.channelInitializer().init(id);
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        ForwardClientNioSocketChannel channel = (ForwardClientNioSocketChannel) ctx.channel();
        int msgOrder = getChannelNextMsgOrder(ctx.channel());
        channel.release();
        if (!Boolean.TRUE.equals(channel.attr(CLEAR_KEY).get())) {
            subscriber.handleEvent(ForwardEvent.closeEvent(channel.longId(), msgOrder));
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("exceptionCaught 异常：", cause);
        ctx.close();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, @ByteBufHandling(ByteBufAction.KEEP) ByteBuf msg) {
        ForwardClientNioSocketChannel channel = (ForwardClientNioSocketChannel) ctx.channel();
        int msgOrder = getChannelNextMsgOrder(channel);
        subscriber.handleEvent(ForwardEvent.messageEvent(channel.longId(), msgOrder, BufUtil.copy(msg)));
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

    private String pollId(Channel channel) {
        while (true) {
            String id = channel.attr(ID_KEY).get();
            if (id != null) {
                return id;
            } else {
                Thread.onSpinWait();
            }
        }
    }

}
