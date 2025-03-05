package com.ytycc;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class AnalyzerRegistry {
    private final List<Analyzer<?>> analyzers = new ArrayList<>();

    private final List<Object> instances = new ArrayList<>();

    public void addAnalyzer(Analyzer<?> analyzer) {
        analyzers.add(analyzer);
    }

    public void addInstance(Object instance) {
        instances.add(instance);
    }

    public String generateReport() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        String timestamp = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        StringBuilder reportBuilder = new StringBuilder("=== Analysis Report ===\n");
        reportBuilder.append("Generated at: ").append(timestamp).append("\n\n");
        Map<Class<?>, Integer> instanceCounters = new HashMap<>();

        for (Object instance : instances) {
            Class<?> clazz = instance.getClass();
            int instanceNumber = instanceCounters.merge(clazz, 1, Integer::sum);
            Optional<Analyzer<?>> optionalAnalyzer = analyzers.stream()
                    .filter(analyzer -> analyzer.acceptAnalyzer(instance))
                    .findFirst();

            if (optionalAnalyzer.isPresent()) {
                @SuppressWarnings("unchecked")
                Analyzer<Object> analyzer = (Analyzer<Object>) optionalAnalyzer.get();

                reportBuilder.append(clazz.getSimpleName())
                        .append("#")
                        .append(instanceNumber)
                        .append(" Analysis:\n")
                        .append(analyzer.analysis(instance).report())
                        .append("\n");
            } else {
                reportBuilder.append(clazz.getSimpleName())
                        .append("#")
                        .append(instanceNumber)
                        .append(" Analysis:\n")
                        .append("No analyzer found for instance.\n\n");
            }
        }

        return reportBuilder.toString();

    }
}
