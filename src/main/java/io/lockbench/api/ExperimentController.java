package io.lockbench.api;

import io.lockbench.api.dto.ExperimentRequest;
import io.lockbench.api.dto.ExperimentResponse;
import io.lockbench.api.dto.MatrixRunRequest;
import io.lockbench.api.dto.MatrixRunResponse;
import io.lockbench.application.ExperimentOrchestrator;
import io.lockbench.application.MatrixExperimentOrchestrator;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/experiments")
public class ExperimentController {

    private final ExperimentOrchestrator experimentOrchestrator;
    private final MatrixExperimentOrchestrator matrixExperimentOrchestrator;

    public ExperimentController(
            ExperimentOrchestrator experimentOrchestrator,
            MatrixExperimentOrchestrator matrixExperimentOrchestrator
    ) {
        this.experimentOrchestrator = experimentOrchestrator;
        this.matrixExperimentOrchestrator = matrixExperimentOrchestrator;
    }

    @PostMapping("/run")
    public ExperimentResponse run(@Valid @RequestBody ExperimentRequest request) {
        return experimentOrchestrator.run(request);
    }

    @PostMapping("/matrix-run")
    public MatrixRunResponse runMatrix(@Valid @RequestBody MatrixRunRequest request) {
        return matrixExperimentOrchestrator.runMatrix(request);
    }
}
