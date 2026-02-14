package io.lockbench.application;

import io.lockbench.api.dto.ExperimentRequest;
import io.lockbench.api.dto.ExperimentResponse;
import io.lockbench.api.dto.MatrixRunRequest;
import io.lockbench.api.dto.MatrixRunResponse;
import io.lockbench.api.dto.MatrixScenarioResult;
import io.lockbench.concurrency.lock.LockStrategyType;
import io.lockbench.concurrency.thread.ThreadModelType;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class MatrixExperimentOrchestrator {

    private final ExperimentOrchestrator experimentOrchestrator;

    public MatrixExperimentOrchestrator(ExperimentOrchestrator experimentOrchestrator) {
        this.experimentOrchestrator = experimentOrchestrator;
    }

    public MatrixRunResponse runMatrix(MatrixRunRequest request) {
        String matrixRunId = UUID.randomUUID().toString();
        List<MatrixScenarioResult> scenarios = new ArrayList<>();
        int successScenarios = 0;
        int failedScenarios = 0;

        for (ThreadModelType threadModel : ThreadModelType.values()) {
            for (LockStrategyType lockStrategy : LockStrategyType.values()) {
                try {
                    ExperimentResponse result = experimentOrchestrator.run(new ExperimentRequest(
                            threadModel,
                            lockStrategy,
                            request.productId(),
                            request.initialStock(),
                            request.quantity(),
                            request.totalRequests(),
                            1,
                            request.optimisticRetries()
                    ));
                    scenarios.add(new MatrixScenarioResult(
                            threadModel.name(),
                            lockStrategy.name(),
                            "SUCCESS",
                            null,
                            result
                    ));
                    successScenarios++;
                } catch (Exception e) {
                    scenarios.add(new MatrixScenarioResult(
                            threadModel.name(),
                            lockStrategy.name(),
                            "FAILED",
                            e.getMessage(),
                            null
                    ));
                    failedScenarios++;
                }
            }
        }

        return new MatrixRunResponse(
                matrixRunId,
                scenarios.size(),
                successScenarios,
                failedScenarios,
                scenarios
        );
    }
}
