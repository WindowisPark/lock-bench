package io.lockbench.api.dto;

import java.time.Instant;

public record ExperimentRunSnapshot(
        String runId,
        Instant storedAt,
        ExperimentResponse result
) {
}
