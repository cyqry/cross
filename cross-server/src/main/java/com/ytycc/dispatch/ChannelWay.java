package com.ytycc.dispatch;


import com.ytycc.dispatch.tertiumquid.pool.ForwardChannelServerPool;
import com.ytycc.dispatch.tertiumquid.manager.ReceiveServerChannelManager;
import com.ytycc.utils.SpringUtil;

public class ChannelWay {
    ForwardServer forwardServer;

    ReceiveServer receiveServer;

    ReceiveServerChannelManager receiveServerChannelManager;

    ForwardChannelServerPool forwardChannelServerPool;


    public ChannelWay(int receiverPort) {
        this.forwardServer = SpringUtil.getBean(ForwardServer.class);
        this.receiveServer = new ReceiveServer(receiverPort);
        receiveServerChannelManager = new ReceiveServerChannelManager();
        forwardChannelServerPool = new ForwardChannelServerPool();
    }

    public void openWay() {
        forwardServer.openForward(receiveServerChannelManager, forwardChannelServerPool);
        receiveServer.openReceiver(receiveServerChannelManager, forwardChannelServerPool);
        forwardChannelServerPool.aliveDetection();
    }
}
