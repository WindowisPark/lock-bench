package io.lockbench.api.dto;

import java.time.Instant;

public record MatrixRunSnapshot(
        String matrixRunId,
        Instant storedAt,
        MatrixRunResponse result
) {
}
