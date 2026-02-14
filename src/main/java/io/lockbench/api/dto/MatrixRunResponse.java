package io.lockbench.api.dto;

import java.util.List;

public record MatrixRunResponse(
        String matrixRunId,
        int totalScenarios,
        int successScenarios,
        int failedScenarios,
        List<MatrixScenarioResult> scenarios
) {
}
