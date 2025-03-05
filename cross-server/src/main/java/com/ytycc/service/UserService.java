package com.ytycc.service;

import com.ytycc.annotations.ByteBufAction;
import com.ytycc.annotations.ByteBufHandling;
import com.ytycc.config.CredentialsProvider;
import com.ytycc.utils.AuthUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private CredentialsProvider credentialsProvider;

    /**
     * 校验并携带上为channel携带上校验信息
     */
    public boolean authenticate(Channel channel, @ByteBufHandling(ByteBufAction.KEEP) ByteBuf buf) {
        if (buf == null || buf.readableBytes() < 5) {
            return falseAuth(channel);
        }

        Optional<String> preToken = getPreToken(buf);
        if (preToken.isEmpty()) {
            return falseAuth(channel);
        }
        Optional<String> finalToken = AuthUtil.finalToken(preToken.get());
        if (finalToken.isEmpty()) {
            return falseAuth(channel);
        }

        if (credentialsProvider.idToken().equals(finalToken.get())) {
            return trueAuth(channel);
        } else {
            return falseAuth(channel);
        }

    }

    private boolean falseAuth(Channel channel) {
        return false;
    }

    private boolean trueAuth(Channel channel) {
        return true;
    }


    private Optional<String> getPreToken(@ByteBufHandling(ByteBufAction.KEEP) ByteBuf buf) {
        if (buf == null || buf.readableBytes() <= 5) {
            return Optional.empty();
        }
        buf.markReaderIndex();
        try {
            int len = buf.readInt();
            byte[] bytes = new byte[len];
            buf.readBytes(bytes);
            return Optional.of(new String(bytes, StandardCharsets.UTF_8));
        } catch (Exception e) {
            return Optional.empty();
        } finally {
            buf.resetReaderIndex();
        }
    }

}
