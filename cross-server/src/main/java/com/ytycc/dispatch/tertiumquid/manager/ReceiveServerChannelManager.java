package com.ytycc.dispatch.tertiumquid.manager;

import com.ytycc.dispatch.tertiumquid.ReceiveServerChannelNioSocketChannel;
import com.ytycc.utils.BufUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import org.springframework.util.Assert;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;

import static com.ytycc.constant.AttrConst.NO_NOTIFY_KEY;


public class ReceiveServerChannelManager {
    public final ConcurrentMap<String, Channel> map = new ConcurrentHashMap<>();

//    private final ChannelFutureListener remover = future -> removeByChannel(future.channel());
//    public final ConcurrentHashMap<ChannelEntry, String> valueKeyMap = new ConcurrentHashMap<>();


    public ReceiveServerChannelManager() {
    }

    public void clear() {
        map.forEach((k, c) -> {
            c.attr(NO_NOTIFY_KEY).set(true);
            c.close();
        });
        map.clear();
    }

    public boolean put(String id, Channel channel) {
        Assert.hasText(id, "id must not be empty");
        Assert.notNull(channel, "channel must not be null");
        return map.putIfAbsent(id, channel) == null;
    }


    public Optional<Channel> getChannel(String id) {
        Channel channel = map.get(id);
        return Optional.ofNullable(channel);
    }



    public Optional<Channel> remove(String id) {
        return Optional.ofNullable(map.remove(id));
    }

    public int size() {
        return map.size();
    }
//
//    //event存在 被删除channel时未 release buf的情况
//    public static class Event {
//
//        private int type;
//
//        private Event() {
//        }
//
//        ByteBuf buf;
//
//        private Event(ByteBuf buf) {
//            this.buf = buf;
//        }
//
//        public void release() {
//            if (buf != null) {
//                buf.release();
//                buf = null;
//            }
//        }
//
//        public static Event CloseEvent() {
//            Event event = new Event();
//            event.type = 0;
//            return event;
//        }
//
//        public ByteBuf takeBuf() {
//            if (type == 0) {
//                throw new IllegalArgumentException("close事件没有buf");
//            }
//            ByteBuf buf = this.buf;
//            this.buf = null;
//            return buf;
//        }
//
//        public byte[] copyBuf() {
//            if (this.buf == null) {
//                throw new RuntimeException("该event没有数据");
//            }
//            byte[] bytes = new byte[this.buf.readableBytes()];
//            this.buf.getBytes(0, bytes);
//            return bytes;
//        }
//
//        public static Event WriteEvent(ByteBuf buf) {
//            Event event = new Event(BufUtil.copy(buf));
//            event.type = 1;
//            return event;
//        }
//
//        public boolean isWrite() {
//            return type == 1;
//        }

//        public boolean isClose() {
//            return type == 0;
//        }

//        @Override
//        public String toString() {
//            if (isWrite()) {
//                byte[] bytes = new byte[buf.readableBytes()];
//                buf.getBytes(0, bytes);
//                return "写事件: 内容:" + new String(bytes, StandardCharsets.UTF_8);
//            } else {
//                return "关闭事件";
//            }
//        }
//    }

//    private static class ChannelEntry {
//
//        private Channel channel;
//
//        private int next;
//
//        private final Map<Integer, String> recordMap = new ConcurrentHashMap<>();
//
//        private final Map<Integer, Event> eventMap = new ConcurrentHashMap<>();
//
//
//        public ChannelEntry() {
//            //test
//        }
//
//        public ChannelEntry(Channel channel) {
//            this.channel = channel;
//        }
//
//        public void release() {
//            eventMap.forEach((k, v) -> {
//                v.release();
//            });
//            eventMap.clear();
//        }
//
//        //保证同一channel上的事件按编号顺序执行
//        public boolean run(int order, Event event) {
//            if (order == next) {
//                if (event.isWrite()) {
//                    channel.writeAndFlush(event.takeBuf());
//                } else if (event.isClose()) {
//                    channel.close();
//                    eventMap.forEach((od, e) -> {
//                        if (e.isWrite()) {
//                            if (od <= order) {
//                                ERROR_LOGGER.info("不应该有关闭事件序号更小的响应事件");
//                            }
//                            e.release();
//                        }
//                    });
//                    eventMap.clear();
//                    return true;
//                }
//                eventMap.remove(order);
//                next++;
//                if (eventMap.containsKey(next)) {
//                    return run(next, eventMap.get(next));
//                }
//            } else if (order > next) {
//                eventMap.put(order, event);
//            } else {
//                event.release();
//                ERROR_LOGGER.info("已经执行过的事件,order:{},next:{},event：{}", order, next, event);
//                return false;
//            }
//            return true;
//        }
//
//
//        public Channel getChannel() {
//            return channel;
//        }
//
//        public void setChannel(Channel channel) {
//            this.channel = channel;
//        }
//
//    }

}
