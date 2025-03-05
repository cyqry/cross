package com.ytycc.dispatch.handler.forward;

import com.ytycc.annotations.ByteBufAction;
import com.ytycc.annotations.ByteBufHandling;
import com.ytycc.dispatch.message.Frame;
import com.ytycc.dispatch.message.Protocol;
import com.ytycc.dispatch.tertiumquid.ReceiveServerChannelEnhancer;
import com.ytycc.dispatch.tertiumquid.manager.ReceiveServerChannelManager;
import com.ytycc.dispatch.tertiumquid.pool.ForwardChannelServerPool;
import com.ytycc.utils.ChannelUtil;
import com.ytycc.utils.ConnectUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static com.ytycc.log.RunLog.ERROR_LOGGER;
import static com.ytycc.log.RunLog.INFO_LOGGER;

public class ForwardTransferHandler extends SimpleChannelInboundHandler<ByteBuf> {
    private final ReceiveServerChannelManager receiveServerChannelManager;
    private final ForwardChannelServerPool forwardChannelServerPool;

    public ForwardTransferHandler(ReceiveServerChannelManager receiveServerChannelManager, ForwardChannelServerPool forwardChannelServerPool) {
        this.receiveServerChannelManager = receiveServerChannelManager;
        this.forwardChannelServerPool = forwardChannelServerPool;
    }


    @Override
    public void channelRead0(ChannelHandlerContext ctx, @ByteBufHandling(ByteBufAction.KEEP) ByteBuf respBuf) throws InterruptedException {
        Protocol.Entry entry = Protocol.decode(respBuf).orElseThrow(() -> new IllegalStateException("错误的协议内容"));

        String id = entry.getId();
        int msgOrder = entry.getMsgOrder();
        Frame frame = entry.getFrame();
        ByteBuf buf = frame.content();

        try {
            switch (frame.code()) {
                case MESSAGE -> {
                    Optional<Channel> channel = receiveServerChannelManager.getChannel(id);
                    if (channel.isPresent()) {
                        ReceiveServerChannelEnhancer enhancer = ChannelUtil.resolveReceiveServerChannel(channel.get());
                        enhancer.writeByOrder(msgOrder, buf)
                                .addListener(f -> {
                                    if (!f.isSuccess()) {
                                        ERROR_LOGGER.info("响应消息到外界失败,id:{}", enhancer.longId());
                                    }
                                });
                        frame.shouldRelease(false);
                    } else {
                        ERROR_LOGGER.error("表中无此id! id:{},msgOrder:{}", id, msgOrder);
                    }
                }
                case CLOSE -> {
                    Optional<Channel> channel = receiveServerChannelManager.getChannel(id);

                    if (channel.isPresent() && ConnectUtil.isActive(channel.get())) {
                        ReceiveServerChannelEnhancer enhancer = ChannelUtil.resolveReceiveServerChannel(channel.get());
                        if (msgOrder > -1) {
                            //正常关闭
                            INFO_LOGGER.info("id:{},消息序号:{},执行closeByOrder", id, msgOrder);
                            enhancer.closeByOrder(msgOrder)
                                    .addListener(f -> {
                                        if (!f.isSuccess()) {
                                            ERROR_LOGGER.error("通过关闭帧关闭id为{}的channel失败,关闭帧序号为{}", id, msgOrder, f.cause());
                                        }
                                    });
                        } else {
                            //强制关闭
                            INFO_LOGGER.info("id:{},消息序号:{},执行强制关闭", id, msgOrder);
                            channel.get().close();
                        }
                    }
                }
                case OPEN_ACK -> {
                }
                case CLOSE_ACK -> {
                }
                case TEST_REPLY -> {
                }
            }
        } finally {
            frame.release();
        }

    }


    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        Channel channel = ctx.channel();
        if (forwardChannelServerPool.remove(channel)) {
            System.out.println(channel + "服务端与客户端连接断开...");
            if (forwardChannelServerPool.isEmpty()) {
                //保证客户端重启，之前的消息不会发送到新的实例 ,暂时实现使用一个标志保证，后续使用 avatar id
                receiveServerChannelManager.clear();
                System.out.println("一个客户端断开了所有连接");
            }
        }
        super.channelInactive(ctx);
    }


}
