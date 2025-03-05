package com.ytycc.dispatch;

import com.ytycc.dispatch.tertiumquid.ReceiveServerNioServerSocketChannel;
import com.ytycc.dispatch.tertiumquid.pool.ForwardChannelServerPool;
import com.ytycc.dispatch.tertiumquid.manager.ReceiveServerChannelManager;
import com.ytycc.dispatch.handler.receive.ReceiveTransferHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;


/**
 * 接收外界连接和消息
 */
public class ReceiveServer {

    private final int port;

//    private final ChannelGroup channels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    public ReceiveServer(int port) {
        this.port = port;
    }

    public void openReceiver(ReceiveServerChannelManager receiveServerChannelManager, ForwardChannelServerPool forwardChannelServerPool) {
        NioEventLoopGroup group = new NioEventLoopGroup();
        new Thread(() -> {
            try {
                ChannelFuture future = new ServerBootstrap()
                        .channel(ReceiveServerNioServerSocketChannel.class)
                        //支持同时的更多并发连接
                        .group(group)
                        .childHandler(new ChannelInitializer<NioSocketChannel>() {
                            @Override
                            protected void initChannel(NioSocketChannel nioSocketChannel) {
                                ChannelPipeline pipeline = nioSocketChannel.pipeline();
                                pipeline.addLast(new ReceiveTransferHandler(forwardChannelServerPool, receiveServerChannelManager));
                            }

                        })
                        .bind(port)
                        .sync();
                System.out.println("接收端:" + port + ",开启成功!");
                future.channel().closeFuture().sync();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } finally {
                group.shutdownGracefully();
            }
        }).start();


    }


}
