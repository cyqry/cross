package com.ytycc.dispatch;

import com.google.common.util.concurrent.RateLimiter;
import com.ytycc.dispatch.handler.forward.ForwardTransferHandler;
import com.ytycc.dispatch.tertiumquid.ForwardClientChannel;
import com.ytycc.dispatch.tertiumquid.ForwardClientNioSocketChannel;
import com.ytycc.dispatch.tertiumquid.ForwardEvent;
import com.ytycc.utils.RateTracker;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.internal.StringUtil;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;

import static com.ytycc.constant.Const.ID_KEY;


public class ForwardClient {


    private final RateTracker rateTracker = new RateTracker(new SimpleMeterRegistry(), "connect_real_server", 1);

    private static final Logger log = LoggerFactory.getLogger(ForwardClient.class);

    private static final int PERMITS = 400;

    //                                        (1s / PERMITS)提供一个令牌.所以PERMITS越大速率越快
    private final RateLimiter connectRateLimiter = RateLimiter.create(PERMITS);

    public final Bootstrap bootstrap;


    private final String realServerHost;
    private final int realServerPort;

    private ForwardClient(String realServerHost, int realServerPort, Subscriber<ForwardEvent> subscriber) {
        NioEventLoopGroup group = new NioEventLoopGroup();
        this.realServerPort = realServerPort;
        this.realServerHost = realServerHost;
        bootstrap = new Bootstrap()
                .group(group)
                .channel(ForwardClientNioSocketChannel.class)
                .handler(new ChannelInitializer<NioSocketChannel>() {

                    @Override
                    protected void initChannel(NioSocketChannel nioSocketChannel) {
                        ChannelPipeline pipeline = nioSocketChannel.pipeline();
                        pipeline.addLast(new ForwardTransferHandler(subscriber));
                    }
                });
    }


    public Optional<ForwardClientChannel> connectRealServer(String id) {

        //限流
        connectRateLimiter.acquire();

        //统计速率
        rateTracker.record();
        try {
            ChannelFuture future = bootstrap.connect(realServerHost, realServerPort);
            future.channel().attr(ID_KEY).set(id);
            future.sync();
            return Optional.of((ForwardClientChannel) future.channel());
        } catch (Exception e) {
            log.error("连接本地服务器失败,err:{}", e.getMessage());
            return Optional.empty();
        }
    }

    public static class Builder {
        private Integer realServerPort;
        private String realServerHost;
        private Subscriber<ForwardEvent> subscriber;

        public Builder realServerPort(int port) {
            this.realServerPort = port;
            return this;
        }

        public Builder subscriber(Subscriber<ForwardEvent> subscriber) {
            this.subscriber = subscriber;
            return this;
        }

        public Builder withConfig(Config config) {
            this.realServerPort = config.realServerPort;
            this.realServerHost = config.realServerHost;
            return this;
        }

        public ForwardClient build() {
            Assert.assertNotNull("realServerHost must be not null", realServerHost);
            Assert.assertNotNull("realServerPort must be not null ", realServerPort);
            Assert.assertNotNull("subscriber must be not null ", subscriber);
            return new ForwardClient(realServerHost, realServerPort, subscriber);
        }
    }

    public record Config(String realServerHost, int realServerPort) {
    }


}
