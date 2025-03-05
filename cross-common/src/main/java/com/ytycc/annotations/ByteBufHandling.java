package com.ytycc.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;



// 注解定义，明确 ByteBuf 的生命周期管理
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface ByteBufHandling {
    ByteBufAction value();
}
