package com.ytycc.constant;

import com.ytycc.dispatch.tertiumquid.ReceiveServerChannelEnhancer;
import io.netty.util.AttributeKey;

public class AttrConst {

    public static final AttributeKey<String> CLIENT_IP_KEY = AttributeKey.valueOf("clientIp");
    public static final AttributeKey<ReceiveServerChannelEnhancer> EXTEND_KEY =
            AttributeKey.valueOf("EXTEND");

    public static final AttributeKey<Boolean> NO_NOTIFY_KEY =
            AttributeKey.valueOf("NO_NOTIFY");
}
