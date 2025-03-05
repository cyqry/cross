package com.ytycc.dispatch;

import com.ytycc.AnalyzeUtil;
import com.ytycc.LocalMessageStoreAnalyzer;
import com.ytycc.annotations.ByteBufAction;
import com.ytycc.annotations.ByteBufHandling;
import io.netty.buffer.ByteBuf;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

//todo
public class LocalMessageStore {
    private final Map<String, Deque<Message>> map = new ConcurrentHashMap<>();
    private final Set<String> closedIds = ConcurrentHashMap.newKeySet();


    {
        AnalyzeUtil.addInstance(this);
    }

    public void idClosed(String id) {
        closedIds.add(id);
        remove(id);
    }

    public Deque<Message> takeCache(String id) {
        return map.remove(id);
    }


    public void tryPullCache(String id, int msgOrder, @ByteBufHandling(ByteBufAction.RELEASE) ByteBuf buf) {
        //判断一定是连接open 前才能缓存close. 如果已经关闭，则没必要加入缓存
        if (!closedIds.contains(id)) {
            map.computeIfAbsent(id, k -> new ArrayDeque<>()).add(new Message(id, msgOrder, buf));
        } else {
            if (buf != null) {
                buf.release();
            }
        }
    }

    public record Message(String id, int msgOrder, ByteBuf buf) {
    }


    private void remove(String id) {
        Deque<Message> remove = map.remove(id);
        if (remove != null) {
            for (Message message : remove) {
                if (message.buf != null && message.buf.refCnt() > 0) {
                    message.buf.release();
                }
            }
        }
    }
}

