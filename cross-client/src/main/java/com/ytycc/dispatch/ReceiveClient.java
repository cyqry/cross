package com.ytycc.dispatch;

import com.google.common.collect.Sets;
import com.ytycc.dispatch.handler.receive.HeartbeatHandler;
import com.ytycc.dispatch.handler.receive.ReceiveTransferHandler;
import com.ytycc.dispatch.message.Frame;
import com.ytycc.dispatch.message.FrameCode;
import com.ytycc.dispatch.message.Protocol;
import com.ytycc.dispatch.tertiumquid.ForwardChannel;
import com.ytycc.dispatch.tertiumquid.ForwardEvent;
import com.ytycc.dispatch.tertiumquid.pool.ForwardChannelPool;
import com.ytycc.dispatch.tertiumquid.manager.ForwardClientChannelManager;
import com.ytycc.strategy.keylock.KeyLockStrategy;
import com.ytycc.strategy.processing.DirectConsumptionStrategy;
import com.ytycc.strategy.processing.EventConsumptionStrategy;
import com.ytycc.strategy.processing.ParallelConsumptionStrategy;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.timeout.IdleStateHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;


public class ReceiveClient implements ForwardChannelProducer, Subscriber<ForwardEvent> {
    private static final Logger log = LoggerFactory.getLogger(ReceiveClient.class);


    private final int serverForwardPort;
    private final String host;
    private final ForwardClientChannelManager forwardClientChannelManager;
    private final ForwardChannelPool forwardChannelPool;
    private final Bootstrap bootstrap;
    private final AtomicInteger test = new AtomicInteger();
    private final KeyLockStrategy keyLockStrategy;
    private final EventConsumptionStrategy<ForwardEvent> eventConsumptionStrategy;
    private final LocalMessageStore store;

    private ReceiveClient(String host, int serverForwardPort, ForwardClient.Config config, KeyLockStrategy keyLockStrategy) {
        this.serverForwardPort = serverForwardPort;
        this.host = host;
        this.keyLockStrategy = keyLockStrategy;
        this.eventConsumptionStrategy = new ParallelConsumptionStrategy<>();
        this.forwardChannelPool = new ForwardChannelPool(this);
        ForwardClient.Builder builder = new ForwardClient.Builder();
        ForwardClient forwardClient = builder.withConfig(config)
                .subscriber(this)
                .build();

        this.forwardClientChannelManager = new ForwardClientChannelManager();
        this.store = new LocalMessageStore();

        NioEventLoopGroup group = new NioEventLoopGroup();
        this.bootstrap = new Bootstrap()
                .group(group)
                .channel(NioSocketChannel.class)
                //设置写缓存高低水位线
                .option(ChannelOption.WRITE_BUFFER_WATER_MARK, new WriteBufferWaterMark(1024 * 1024, 8 * 1024 * 1024))
                .handler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    protected void initChannel(NioSocketChannel nioSocketChannel) {
                        ChannelPipeline pipeline = nioSocketChannel.pipeline();
                        pipeline.addLast(new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE, 0, 4, 0, 4));
                        pipeline.addLast(new IdleStateHandler(45, 15, 0));
                        pipeline.addLast(new HeartbeatHandler());
                        pipeline.addLast(new ReceiveTransferHandler(forwardChannelPool, forwardClient, store, keyLockStrategy, forwardClientChannelManager));
                    }
                });

    }

    public static ReceiveClient create(String host, int serverForwardPort, ForwardClient.Config config, KeyLockStrategy keyLockStrategy) {
        return new ReceiveClient(host, serverForwardPort, config, keyLockStrategy);
    }


    public void establish() {
        new Thread(forwardChannelPool::unAliveDetection).start();
        new Thread(forwardChannelPool::fill).start();
    }


    @Override
    public ChannelFuture connectForwardServer() {
        return bootstrap
                .connect(new InetSocketAddress(host, serverForwardPort));
    }


    @Override
    public void handleEvent(ForwardEvent event) {
        eventConsumptionStrategy.process(event, this::handleEvent0);
    }

    private void handleEvent0(ForwardEvent event) {
        if (event.isClose()) {
            keyLockStrategy.executeSafelyWithKey(event.channelId(), () -> {
                //记录已经关闭的id,并清理该id的消息缓存
                store.idClosed(event.channelId());
                return Void.class;
            });

            forwardClientChannelManager.remove(event.channelId());
            Optional<ForwardChannel> resource = forwardChannelPool.findResource();
            if (resource.isEmpty()) {
                log.error("未连接到服务器,发送关闭帧失败,id:{}", event.channelId());
                return;
            }
            resource.get().writeCloseFrame(event.channelId(), event.msgOrder()).addListener(f -> {
                keyLockStrategy.deleteKey(event.channelId());
                if (!f.isSuccess()) {
                    log.error("发送关闭帧失败, id:{}", event.channelId(), f.cause());
                }
            });
        } else if (event.isMessage()) {
            Optional<ForwardChannel> resource = forwardChannelPool.findResource();
            if (resource.isEmpty()) {
                log.error("响应返回给Server失败,id:{}", event.channelId());
                return;
            }

            Frame frame = new Frame(FrameCode.MESSAGE, event.takeBuf());
            resource.get()
                    .writeAndFlush(Protocol.transferFrameEncode(event.channelId(), event.msgOrder(), frame))
                    .addListener(f -> {
                        if (!f.isSuccess()) {
                            log.error("响应返回给外界失败,id:{}", event.channelId(), f.cause());
                        }
                    });
            frame.release();
        }
        event.release();
    }
}
