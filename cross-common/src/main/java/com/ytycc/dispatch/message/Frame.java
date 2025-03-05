package com.ytycc.dispatch.message;

import com.ytycc.annotations.ByteBufAction;
import com.ytycc.annotations.ByteBufHandling;
import com.ytycc.dispatch.tertiumquid.Release;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;

import java.util.Objects;
import java.util.Optional;

/**
 * code  1.关闭帧 2.开启帧 3.消息帧 ...
 */

public final class Frame implements Release {
    private final FrameCode code;
    private final ByteBuf content;
    private boolean shouldRelease = true;

    /**
     *
     */
    public Frame(FrameCode code, @ByteBufHandling(ByteBufAction.RELEASE) ByteBuf content) {
        this.code = code;
        this.content = content;
    }


    /**
     * 不会 对Frame内部的buf
     *
     * @return
     */
    public ByteBuf toBuf() {
        if (content != null) {
            ByteBuf heapBuffer = ByteBufAllocator.DEFAULT.heapBuffer();
            heapBuffer.writeShort(code.code());
            content.markReaderIndex();
            heapBuffer.writeBytes(content);
            content.resetReaderIndex();
            return heapBuffer;
        } else {
            throw new RuntimeException("content不应该为空");
        }

    }

    public static Optional<Frame> from(@ByteBufHandling(ByteBufAction.KEEP) ByteBuf buf) {
        if (buf == null || buf.readableBytes() < 2) {
            return Optional.empty();
        }
        buf.markReaderIndex();
        short code = buf.readShort();
        ByteBuf content = ByteBufAllocator.DEFAULT.heapBuffer(buf.readableBytes());
        buf.readBytes(content);
        Optional<FrameCode> frameCode = FrameCode.from(code);

        buf.resetReaderIndex();
        return frameCode.map(value -> new Frame(value, content));
    }

    @Override
    public void release() {
        if (shouldRelease && content != null) {
            content.release();
        }
    }


    public void shouldRelease(boolean should) {
        this.shouldRelease = should;
    }

    @Override
    public String toString() {
        return "Frame{" +
                "code=" + code +
                ", content=" + content +
                '}';
    }

    public FrameCode code() {
        return code;
    }

    public ByteBuf content() {
        return content;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (Frame) obj;
        return Objects.equals(this.code, that.code) &&
                Objects.equals(this.content, that.content);
    }

    @Override
    public int hashCode() {
        return Objects.hash(code, content);
    }

}
