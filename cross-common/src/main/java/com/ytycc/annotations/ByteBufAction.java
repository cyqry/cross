package com.ytycc.annotations;

public enum ByteBufAction {
    RELEASE, // buf转移所有权或者被释放，不可再用
    KEEP     // 保持原样
}
