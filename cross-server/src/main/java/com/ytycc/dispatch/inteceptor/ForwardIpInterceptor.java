package com.ytycc.dispatch.inteceptor;

import com.ytycc.manager.IpStateManager;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.AttributeKey;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

import static com.ytycc.constant.AttrConst.CLIENT_IP_KEY;
import static com.ytycc.log.RunLog.ERROR_LOGGER;
import static com.ytycc.log.RunLog.INFO_LOGGER;


public class ForwardIpInterceptor extends ChannelDuplexHandler {

    //当前允许连接的唯一 IP
    private static volatile String currentConnectedIp = null;

    //当前连接数
    private static final AtomicInteger connectionCount = new AtomicInteger(0);

    private final IpStateManager ipStateManager;

    public ForwardIpInterceptor(IpStateManager ipStateManager) {
        this.ipStateManager = ipStateManager;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        InetSocketAddress remoteAddress = (InetSocketAddress) ctx.channel().remoteAddress();
        String clientIp = remoteAddress.getAddress().getHostAddress();
        ctx.channel().attr(CLIENT_IP_KEY).set(clientIp);
        connectionCount.incrementAndGet();
        //ip 限制
        if (!uniqueIp(clientIp)) {
            System.out.println("拒绝连接，IP: " + clientIp + " (当前仅允许 " + currentConnectedIp + " 连接)");
            ctx.close();
            return;
        }

        //错误次数限制
        IpStateManager.IpState state = ipStateManager.getIpState(clientIp);
        if (state.hasReachedErrorMaxCount()) {
            System.out.println("拒绝连接：IP: " + clientIp + " (超过错误限制)");
            ctx.close();
            return;
        }

        System.out.println("IP: " + clientIp);

        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        String clientIp = ctx.channel().attr(CLIENT_IP_KEY).get();
        if (clientIp != null && clientIp.equals(currentConnectedIp)) {
            int remainingConnections = connectionCount.decrementAndGet();
            System.out.println("连接断开，IP: " + clientIp + ", 剩余连接数: " + remainingConnections);
            if (remainingConnections == 0) {
                releaseIp(clientIp);
            }
        }
        super.channelInactive(ctx);
    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        INFO_LOGGER.error("错误", cause);
        ctx.close();
    }

    private synchronized boolean uniqueIp(String remoteIp) {
        if (currentConnectedIp == null) {
            currentConnectedIp = remoteIp;
            return true;
        } else return currentConnectedIp.equals(remoteIp);
    }


    private synchronized void releaseIp(String remoteIp) {
        int remainingConnections = connectionCount.get();
        if (remainingConnections == 0) {
            currentConnectedIp = null;
            System.out.println("当前无连接， IP " + remoteIp + " 被释放.");
        }
    }

}
