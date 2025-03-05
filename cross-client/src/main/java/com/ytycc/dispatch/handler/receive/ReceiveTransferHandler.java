package com.ytycc.dispatch.handler.receive;

import com.ytycc.annotations.ByteBufAction;
import com.ytycc.annotations.ByteBufHandling;
import com.ytycc.config.UserInfo;
import com.ytycc.dispatch.ForwardClient;
import com.ytycc.dispatch.LocalMessageStore;
import com.ytycc.dispatch.message.Frame;
import com.ytycc.dispatch.message.Protocol;
import com.ytycc.dispatch.tertiumquid.ForwardChannel;
import com.ytycc.dispatch.tertiumquid.ForwardClientChannel;
import com.ytycc.dispatch.tertiumquid.manager.ForwardClientChannelManager;
import com.ytycc.dispatch.tertiumquid.pool.ForwardChannelPool;
import com.ytycc.strategy.keylock.KeyLockStrategy;
import com.ytycc.utils.AuthUtil;
import com.ytycc.utils.BufUtil;
import com.ytycc.utils.ConnectUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import static com.ytycc.constant.Const.AUTH_KEY;

public class ReceiveTransferHandler extends SimpleChannelInboundHandler<ByteBuf> {
    private static final Logger log = LoggerFactory.getLogger(ReceiveTransferHandler.class);
    private final ForwardChannelPool forwardChannelPool;
    private final ForwardClient forwardClient;
    private final KeyLockStrategy keyLockStrategy;
    private final ForwardClientChannelManager forwardClientChannelManager;
    private final LocalMessageStore store;


    public ReceiveTransferHandler(ForwardChannelPool forwardChannelPool, ForwardClient forwardClient, LocalMessageStore store, KeyLockStrategy keyLockStrategy, ForwardClientChannelManager forwardClientChannelManager) {
        this.forwardClient = forwardClient;
        this.forwardChannelPool = forwardChannelPool;
        this.keyLockStrategy = keyLockStrategy;
        this.forwardClientChannelManager = forwardClientChannelManager;
        this.store = store;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        Channel channel = ctx.channel();
        //让服务端校验身份
        ByteBuf auth = AuthUtil.authEncode(UserInfo.username(), UserInfo.password());
        channel.writeAndFlush(Protocol.transferEncode(auth));
        auth.release();
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        Channel channel = ctx.channel();
        forwardChannelPool.remove(channel);
        if (forwardChannelPool.isEmpty()) {
            log.info("与服务端的连接全部断开");
            forwardClientChannelManager.clear();
        }
        super.channelInactive(ctx);
    }


    @Override
    protected void channelRead0(ChannelHandlerContext ctx, @ByteBufHandling(ByteBufAction.KEEP) ByteBuf msg) {
        Protocol.Entry entry = Protocol.decode(msg).orElseThrow();
        String id = entry.getId();
        int msgOrder = entry.getMsgOrder();
        Frame frame = entry.getFrame();
        ByteBuf buf = frame.content();

        switch (frame.code()) {
            case MESSAGE -> {
                handleOrStoreMessage(id, msgOrder, buf);
                frame.shouldRelease(false);
            }
            case OPEN -> {
                ForwardClientChannel ch = getOrCreate(id,
                        (c) -> {
                            ctx.writeAndFlush(Protocol.openAckFrame(id));
                        });
                if (ch == null) {
                    keyLockStrategy.executeSafelyWithKey(id, () -> {
                        store.idClosed(id);
                        return Void.class;
                    });
                    ctx.writeAndFlush(Protocol.forceCloseFrame(id));
                } else {
                    Deque<LocalMessageStore.Message> deque = store.takeCache(id);
                    if (deque != null) {
                        for (LocalMessageStore.Message message : deque) {
                            if (message.buf() != null) {
                                ch.reqByOrder(message.msgOrder(), message.buf())
                                        .addListener(f -> {
                                            if (!f.isSuccess()) {
                                                log.error("消息请求失败,id:{}", id, f.cause());
                                            }
                                        });

                            } else {
                                log.info("执行缓存中的closeByOrder,id:{},msgOrder:{}", id, msgOrder);
                                ch.closeByOrder(message.msgOrder()).addListener(f -> {
                                    if (!f.isSuccess()) {
                                        log.error("执行缓存中的关闭帧失败,id:{},order:{}", id, message.msgOrder(), f.cause());
                                    }
                                });
                            }
                        }
                    }
                }
            }
            case CLOSE -> {
                handleOrStoreClose(ctx.channel(), id, msgOrder);
            }
            case AUTH -> {
                if ("true".equals(id)) {
                    System.out.println("校验成功");
                    ctx.channel().attr(AUTH_KEY);
                    forwardChannelPool.register(new ForwardChannel(ctx.channel()));
                } else {
                    System.out.println("账号或密码错误");
                    System.exit(0);
                }
            }

            case TEST -> {
                Optional<ForwardChannel> resource = forwardChannelPool.findResource();
                if (resource.isPresent()) {
                    ByteBuf replyFrame = Protocol.testReplyFrame();
                    resource.get().writeAndFlush(replyFrame);
                }
            }

            default -> {

            }
        }
        frame.release();
    }

    private void handleOrStoreClose(Channel fc, String id, int msgOrder) {
        keyLockStrategy.executeSafelyWithKey(id, () -> {
            //实现顺序关闭
            Optional<ForwardClientChannel> channel = forwardClientChannelManager.getForwardClientChannel(id);
            if (channel.isPresent()) {
                if (channel.get().isActive()) {
                    log.info("客户端执行closeByOrder,id:{},msgOrder:{}", id, msgOrder);
                    channel.get().closeByOrder(msgOrder).addListener(f -> {
                        if (!f.isSuccess()) {
                            log.error("客户端执行closeByOrder关闭失败,id:{},msgOrder:{}", id, msgOrder, f.cause());
                        }
                    });
                    //        fc.writeAndFlush(Protocol.closeAckFrame(id));//目前无作用
                }
            } else {
                store.tryPullCache(id, msgOrder, null);
            }
            return Void.class;
        });


    }

    private void handleOrStoreMessage(String uuid, int msgOrder, @ByteBufHandling(ByteBufAction.RELEASE) ByteBuf buf) {
        keyLockStrategy.executeSafelyWithKey(uuid, () -> {
            Optional<ForwardClientChannel> localChannel = forwardClientChannelManager.getForwardClientChannel(uuid);
            if (localChannel.isPresent()) {
                if (ConnectUtil.isInActive(localChannel.get())) {
                    buf.release();
                    return Void.class;
                }

                localChannel.get().reqByOrder(msgOrder, buf)
                        .addListener(f -> {
                            if (!f.isSuccess()) {
                                log.error("请求消息失败,uuid:{},order:{}", uuid, msgOrder);
                            }
                        });
            } else {
                store.tryPullCache(uuid, msgOrder, buf);
            }
            return Void.class;
        });


    }

    private ForwardClientChannel getOrCreate(String uuid, Consumer<Channel> createdCallback) {
        return keyLockStrategy.executeSafelyWithKey(uuid, () -> {
            Optional<ForwardClientChannel> channel = forwardClientChannelManager.getForwardClientChannel(uuid);
            if (channel.isEmpty()) {
                Optional<ForwardClientChannel> newChannel = forwardClient.connectRealServer(uuid);
                if (newChannel.isPresent()) {
                    forwardClientChannelManager.put(uuid, newChannel.get());
                    if (createdCallback != null) {
                        createdCallback.accept(newChannel.get());
                    }
                    return newChannel.get();
                } else {
                    return null;
                }
            }
            log.error("不应该走到这里");
            return channel.get();
        });
    }

}
