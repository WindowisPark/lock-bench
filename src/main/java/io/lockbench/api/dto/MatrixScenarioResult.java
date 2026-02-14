package io.lockbench.api.dto;

public record MatrixScenarioResult(
        String threadModel,
        String lockStrategy,
        String status,
        String message,
        ExperimentResponse result
) {
}
