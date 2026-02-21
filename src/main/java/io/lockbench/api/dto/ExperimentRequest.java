package io.lockbench.api.dto;

import io.lockbench.concurrency.lock.LockStrategyType;
import io.lockbench.concurrency.thread.ThreadModelType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ExperimentRequest(
        @NotNull ThreadModelType threadModel,
        @NotNull LockStrategyType lockStrategy,
        @NotNull Long productId,
        @Min(1) int initialStock,
        @Min(1) int quantity,
        @Min(1) @Max(1_000_000) int totalRequests,
        @Min(1) @Max(20_000) int concurrency,
        @Min(0) @Max(20) int optimisticRetries,
        @Min(0) @Max(10000) int processingDelayMillis
) {
}
