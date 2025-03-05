package com.ytycc.dispatch.message;

import com.ytycc.annotations.ByteBufAction;
import com.ytycc.annotations.ByteBufHandling;
import com.ytycc.utils.LogUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import org.junit.Assert;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Optional;


public class Protocol {

    private static final ByteBuf PING = ping();
    private static final ByteBuf PONG = pong();


    public static ByteBuf ping() {
        Frame frame = new Frame(FrameCode.PING, ByteBufAllocator.DEFAULT.heapBuffer());
        ByteBuf ping = transferFrameEncode("ping", -1, frame);
        frame.release();
        return ping;
    }

    public static ByteBuf pong() {
        Frame frame = new Frame(FrameCode.PONG, ByteBufAllocator.DEFAULT.heapBuffer());
        ByteBuf pong = transferFrameEncode("pong", -1, frame);
        frame.release();
        return pong;
    }

    public static ByteBuf auth(boolean authenticate) {
        Frame frame = new Frame(FrameCode.AUTH, ByteBufAllocator.DEFAULT.heapBuffer());
        ByteBuf auth = transferFrameEncode(String.valueOf(authenticate), -1, frame);
        frame.release();
        return auth;
    }

    @Test
    public void test() {
        LogUtil.log(auth(true));
    }

    public static boolean isPing(ByteBuf buf) {
        return Arrays.equals(buf.array(), PING.array());
    }

    public static boolean isPong(ByteBuf buf) {
        return Arrays.equals(buf.array(), PONG.array());
    }


    //告诉服务端需要等消息再断开
    public static ByteBuf closeFrame(String id, int msgOrder) {
        Frame frame = new Frame(FrameCode.CLOSE, ByteBufAllocator.DEFAULT.heapBuffer());
        ByteBuf close = transferFrameEncode(id, msgOrder, frame);
        frame.release();
        return close;
    }

    //告诉服务端直接断开
    public static ByteBuf forceCloseFrame(String id) {
        Frame frame = new Frame(FrameCode.CLOSE, ByteBufAllocator.DEFAULT.heapBuffer());
        ByteBuf close = transferFrameEncode(id, -1, frame);
        frame.release();
        return close;
    }

    public static ByteBuf closeAckFrame(String uuid) {
        Frame frame = new Frame(FrameCode.CLOSE_ACK, ByteBufAllocator.DEFAULT.heapBuffer());
        ByteBuf closeAck = transferFrameEncode(uuid, -1, frame);
        frame.release();
        return closeAck;
    }

    public static ByteBuf openFrame(String uuid) {
        Frame frame = new Frame(FrameCode.OPEN, ByteBufAllocator.DEFAULT.heapBuffer());
        ByteBuf open = transferFrameEncode(uuid, -1, frame);
        frame.release();
        return open;
    }


    public static ByteBuf openAckFrame(String uuid) {
        Frame frame = new Frame(FrameCode.OPEN_ACK, ByteBufAllocator.DEFAULT.heapBuffer());
        ByteBuf openAck = transferFrameEncode(uuid, -1, frame);
        frame.release();
        return openAck;
    }

    public static ByteBuf testFrame() {
        ByteBuf buffer = ByteBufAllocator.DEFAULT.heapBuffer();
        buffer.writeBytes(
                "test"
                        .repeat(1000).getBytes(StandardCharsets.UTF_8)
        );
        Frame frame = new Frame(FrameCode.TEST, buffer);
        ByteBuf testAck = transferFrameEncode("test", -1, frame);
        frame.release();
        return testAck;
    }

    public static ByteBuf testReplyFrame() {
        ByteBuf buffer = ByteBufAllocator.DEFAULT.heapBuffer();
        buffer.writeBytes(
                "test"
                        .repeat(1000).getBytes(StandardCharsets.UTF_8)
        );
        Frame frame = new Frame(FrameCode.TEST_REPLY, buffer);
        ByteBuf testReplyAck = transferFrameEncode("testReply", -1, frame);
        frame.release();
        return testReplyAck;
    }

    /**
     * 对应LTC解码器
     *
     * @param allContent
     * @return
     */
    public static ByteBuf transferEncode(@ByteBufHandling(ByteBufAction.KEEP) ByteBuf allContent) {
        ByteBuf buffer = ByteBufAllocator.DEFAULT.heapBuffer();
        allContent.markReaderIndex();
        buffer.writeInt(allContent.readableBytes());
        buffer.writeBytes(allContent);
        allContent.resetReaderIndex();
        return buffer;
    }


    /**
     * 在transferEncode的基础上，再封装一层frame
     *
     * @param id
     * @param msgOrder
     * @param frame
     * @return
     */

    public static ByteBuf transferFrameEncode(String id, int msgOrder, Frame frame) {
        Assert.assertNotNull(frame);
        ByteBuf frameBuf = frame.toBuf();

        ByteBuf byteBuf = ByteBufAllocator.DEFAULT.heapBuffer();
        byte[] idBytes = id.getBytes(StandardCharsets.UTF_8);
        int idLen = idBytes.length;
        byteBuf.writeInt(idLen);
        byteBuf.writeBytes(idBytes);
        //消息序号
        byteBuf.writeInt(msgOrder);
        //封装外界消息后的frame
        byteBuf.writeBytes(frameBuf);
        ByteBuf finalBuf = transferEncode(byteBuf);
        frameBuf.release();
        byteBuf.release();
        return finalBuf;
    }


    /**
     * 解码  ltc解码器解码后得到的 frame msg
     */
    public static Optional<Entry> decode(@ByteBufHandling(ByteBufAction.KEEP) ByteBuf msg) {
        msg.markReaderIndex();
        try {
            int uuidLen = msg.readInt();
            String uuid;
            byte[] bytes = new byte[uuidLen];
            msg.readBytes(bytes);
            uuid = new String(bytes, StandardCharsets.UTF_8);
            int msgOrder = msg.readInt();
            Optional<Frame> frame = Frame.from(msg);

            return frame.map(value -> new Entry(value, uuid, msgOrder));
        } catch (IndexOutOfBoundsException e) {
            return Optional.empty();
        } finally {
            msg.resetReaderIndex();
        }
    }

    public static class Entry {
        private final Frame frame;
        private final String id;
        private final int msgOrder;

        public Entry(Frame frame, String id, int msgOrder) {
            this.frame = frame;
            this.id = id;
            this.msgOrder = msgOrder;
        }

        public Frame getFrame() {
            return frame;
        }


        public String getId() {
            return id;
        }


        public int getMsgOrder() {
            return msgOrder;
        }

        @Override
        public String toString() {
            return "Entry{" +
                    "frame=" + frame +
                    ", id='" + id + '\'' +
                    ", msgOrder=" + msgOrder +
                    '}';
        }
    }
}
