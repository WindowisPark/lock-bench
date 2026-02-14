package io.lockbench.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record MatrixRunRequest(
        @NotNull Long productId,
        @Min(1) int initialStock,
        @Min(1) int quantity,
        @Min(1) @Max(1_000_000) int totalRequests,
        @Min(0) @Max(20) int optimisticRetries
) {
}
