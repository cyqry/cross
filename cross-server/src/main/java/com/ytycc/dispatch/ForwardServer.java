package com.ytycc.dispatch;

import com.ytycc.dispatch.tertiumquid.pool.ForwardChannelServerPool;
import com.ytycc.dispatch.tertiumquid.manager.ReceiveServerChannelManager;
import com.ytycc.dispatch.handler.forward.ForwardInitialAuthHandler;
import com.ytycc.dispatch.handler.forward.ForwardTransferHandler;
import com.ytycc.dispatch.handler.forward.HeartbeatHandler;
import com.ytycc.dispatch.inteceptor.ForwardIpInterceptor;
import com.ytycc.manager.IpStateManager;
import com.ytycc.service.UserService;
import com.ytycc.utils.SpringUtil;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.timeout.IdleStateHandler;

import java.util.concurrent.atomic.AtomicInteger;

//单例的
public class ForwardServer {

    private final UserService userService;
    private final int port;


    public static final AtomicInteger testReply = new AtomicInteger();

    public ForwardServer(int port) {
        userService = SpringUtil.getBean(UserService.class);
        this.port = port;
    }


    public void openForward(ReceiveServerChannelManager receiveServerChannelManager, ForwardChannelServerPool forwardChannelServerPool) {
        new Thread(() -> {
            NioEventLoopGroup group = new NioEventLoopGroup();
            IpStateManager ipStateManager = new IpStateManager();

            try {
                ChannelFuture future = new ServerBootstrap()
                        .group(group)
                        //写缓存的高低水位线
                        .option(ChannelOption.WRITE_BUFFER_WATER_MARK, new WriteBufferWaterMark(1024 * 1024, 8 * 1024 * 1024))
                        .channel(NioServerSocketChannel.class)
                        .childHandler(new ChannelInitializer<NioSocketChannel>() {
                            @Override
                            protected void initChannel(NioSocketChannel nioSocketChannel) {
                                ChannelPipeline pipeline = nioSocketChannel.pipeline();
                                pipeline.addLast(new ForwardIpInterceptor(ipStateManager));
                                pipeline.addLast(new IdleStateHandler(45, 15, 0));
                                pipeline.addLast(new HeartbeatHandler());
                                pipeline.addLast(new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE, 0, 4, 0, 4));
                                pipeline.addLast(new ForwardInitialAuthHandler(forwardChannelServerPool, userService, ipStateManager));
                                pipeline.addLast(new ForwardTransferHandler(receiveServerChannelManager, forwardChannelServerPool));

                            }
                        })
                        .bind(port)
                        .sync();

                System.out.println("转发端:" + port + ",开启成功!");
                future.channel().closeFuture().sync();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } finally {
                group.shutdownGracefully();
            }


        }).start();
    }
}
