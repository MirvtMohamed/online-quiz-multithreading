package org.example.utility;

import java.util.concurrent.atomic.AtomicLong;

public class PerformanceLogger {
    private AtomicLong startTime = new AtomicLong();
    private AtomicLong endTime = new AtomicLong();
    private AtomicLong executionTime = new AtomicLong();

    public void start() {
        startTime.set(System.nanoTime());
    }

    public void stop() {
        endTime.set(System.nanoTime());
        executionTime.set(endTime.get() - startTime.get());
    }

    public long getExecutionTime() {
        return executionTime.get();
    }

    public static void logPerformance(String taskName, long timeInNanoSeconds) {
        System.out.printf("%s took %.3f ms%n", taskName, timeInNanoSeconds / 1_000_000.0);
    }
}
