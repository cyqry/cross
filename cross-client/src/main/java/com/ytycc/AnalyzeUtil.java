package com.ytycc;

import java.io.BufferedWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AnalyzeUtil {

    private static final Path OUTPUT_PATH = Paths.get(".\\logs\\analysis.log");

    private static final AnalyzerRegistry aRegistry = new AnalyzerRegistry();
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    static {
        aRegistry.addAnalyzer(new LocalMessageStoreAnalyzer());
    }

    public static void addInstance(Object instance) {
        aRegistry.addInstance(instance);
    }

    public static void startReport() {
        scheduler.scheduleAtFixedRate(AnalyzeUtil::writeLog, 10, 10, TimeUnit.SECONDS);
    }

    private static void writeLog() {
        try {
            String logMessage = aRegistry.generateReport();
            try (BufferedWriter writer = Files.newBufferedWriter(
                    OUTPUT_PATH,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND)) {
                writer.write(logMessage);
                writer.newLine();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void stop() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
