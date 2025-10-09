package org.openrewrite.rpc.request;

import lombok.Value;
import lombok.experimental.Accessors;
import org.jspecify.annotations.Nullable;

@Value
@Accessors(fluent = true)
public class RequestMetrics {
    boolean success;
    long durationMilliseconds;

    @Nullable
    String errorType;

    MemoryMetrics memoryMetrics;

    @Value
    @Accessors(fluent = true)
    public static class MemoryMetrics {
        long heapUsedBytes;
        long heapMaxBytes;
    }

    public static class Sample {
        private final long startTime;
        private final Runtime runtime = Runtime.getRuntime();

        public Sample() {
            this.startTime = System.currentTimeMillis();
        }

        public RequestMetrics complete() {
            long heapUsed = runtime.totalMemory() - runtime.freeMemory();
            long heapMax = runtime.maxMemory();

            return new RequestMetrics(
                    true,
                    System.currentTimeMillis() - startTime,
                    null,
                    new MemoryMetrics(heapUsed, heapMax)
            );
        }

        public RequestMetrics error(Throwable error) {
            long heapUsed = runtime.totalMemory() - runtime.freeMemory();
            long heapMax = runtime.maxMemory();

            return new RequestMetrics(
                    false,
                    System.currentTimeMillis() - startTime,
                    error.getClass().getSimpleName(),
                    new MemoryMetrics(heapUsed, heapMax)
            );
        }
    }
}
