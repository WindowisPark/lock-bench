package io.lockbench.application;

import io.lockbench.api.dto.ExperimentResponse;
import io.lockbench.api.dto.ExperimentRunSnapshot;
import io.lockbench.api.dto.MatrixRunResponse;
import io.lockbench.api.dto.MatrixRunSnapshot;

import java.util.Optional;

public interface RunResultStore {

    void saveExperimentResult(ExperimentResponse response);

    Optional<ExperimentRunSnapshot> findExperimentRun(String runId);

    void saveMatrixRunResult(MatrixRunResponse response);

    Optional<MatrixRunSnapshot> findMatrixRun(String matrixRunId);
}
