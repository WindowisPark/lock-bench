package io.lockbench.api.dto;

import java.util.Map;

public record ExperimentResponse(
        String runId,
        String threadModel,
        String lockStrategy,
        int concurrency,
        int totalRequests,
        int successCount,
        int failureCount,
        Map<String, Integer> failureBreakdown,
        long elapsedMillis,
        long elapsedNanos,
        ThroughputConfidence throughputConfidence,
        double throughputPerSec,
        long p50Millis,
        long p95Millis,
        long p99Millis
) {
}

