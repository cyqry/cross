package com.ytycc;

import com.ytycc.dispatch.LocalMessageStore;
import io.netty.util.internal.TypeParameterMatcher;

import java.util.*;
import java.util.stream.Collectors;

public class LocalMessageStoreAnalyzer extends AbstractAnalyzer<LocalMessageStore> {


    @Override
    Result doAnalysis(ObjectView<LocalMessageStore> view) throws Exception {
        // 获取 map 的值
        Map<String, Deque<LocalMessageStore.Message>> map = view.getValue("map");
        Set<String> closedIds = view.getValue("closedIds");
        // 返回生成的分析报告
        return () -> {
            if (map == null || map.isEmpty()) {
                return "LocalMessageStore Analysis Report:\nThe map is empty or null. No data to analyze.";
            }

            StringBuilder reportBuilder = new StringBuilder("=== LocalMessageStore Analysis Report ===\n\n");

            // 遍历每个 key-value 对
            map.forEach((key, messages) -> {
                reportBuilder.append("Id: ").append(key).append("\n");
                reportBuilder.append("Total Messages: ").append(messages.size()).append("\n");

                // 消息统计信息
                int totalBufferSize = messages.stream()
                        .mapToInt(message -> message.buf() == null ? 0 : message.buf().readableBytes())
                        .sum();

                reportBuilder.append("Total Buffer Size: ").append(totalBufferSize).append(" bytes\n");

                // 按 msgOrder 排序并取前 5 条
                List<LocalMessageStore.Message> topMessages = messages.stream()
                        .sorted(Comparator.comparingInt(LocalMessageStore.Message::msgOrder))
                        .limit(5)
                        .toList();

                reportBuilder.append("Sample Messages (Top 5 by msgOrder):\n");
                for (LocalMessageStore.Message message : topMessages) {
                    reportBuilder.append("  - ID: ").append(message.id())
                            .append(", Order: ").append(message.msgOrder())
                            .append(", Buffer Size: ").append(message.buf() != null ? message.buf().readableBytes() : "null")
                            .append(" bytes\n");
                }

                reportBuilder.append("\n");
            });

            return reportBuilder.toString();
        };
    }
}