package com.ytycc.constant;

import io.netty.util.AttributeKey;

public class Const {
    public static  String Authenticated = "Authenticated";
    public static final AttributeKey<String> AUTH_KEY = AttributeKey.valueOf(Const.Authenticated);
    public static final AttributeKey<String> ID_KEY = AttributeKey.valueOf("ID_KEY");
    public static final AttributeKey<Boolean> CLEAR_KEY = AttributeKey.valueOf("CLEAR_KEY");
}
