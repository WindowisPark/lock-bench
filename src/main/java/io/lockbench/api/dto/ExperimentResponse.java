package io.lockbench.api.dto;

public record ExperimentResponse(
        String runId,
        String threadModel,
        String lockStrategy,
        int totalRequests,
        int successCount,
        int failureCount,
        long elapsedMillis,
        double throughputPerSec,
        long p50Millis,
        long p95Millis,
        long p99Millis
) {
}
