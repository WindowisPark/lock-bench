package io.lockbench.api;

import io.lockbench.api.dto.ExperimentRequest;
import io.lockbench.api.dto.ExperimentResponse;
import io.lockbench.api.dto.ExperimentRunSnapshot;
import io.lockbench.api.dto.MatrixRunRequest;
import io.lockbench.api.dto.MatrixRunResponse;
import io.lockbench.api.dto.MatrixRunSnapshot;
import io.lockbench.application.ExperimentOrchestrator;
import io.lockbench.application.MatrixExperimentOrchestrator;
import io.lockbench.application.RunResultStore;
import io.lockbench.domain.model.StockSnapshot;
import io.lockbench.domain.port.StockAccessPort;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/experiments")
public class ExperimentController {

    private final ExperimentOrchestrator experimentOrchestrator;
    private final MatrixExperimentOrchestrator matrixExperimentOrchestrator;
    private final RunResultStore runResultStore;
    private final StockAccessPort stockAccessPort;

    public ExperimentController(
            ExperimentOrchestrator experimentOrchestrator,
            MatrixExperimentOrchestrator matrixExperimentOrchestrator,
            RunResultStore runResultStore,
            StockAccessPort stockAccessPort
    ) {
        this.experimentOrchestrator = experimentOrchestrator;
        this.matrixExperimentOrchestrator = matrixExperimentOrchestrator;
        this.runResultStore = runResultStore;
        this.stockAccessPort = stockAccessPort;
    }

    @PostMapping("/run")
    public ExperimentResponse run(@Valid @RequestBody ExperimentRequest request) {
        return experimentOrchestrator.run(request);
    }

    @PostMapping("/matrix-run")
    public MatrixRunResponse runMatrix(@Valid @RequestBody MatrixRunRequest request) {
        return matrixExperimentOrchestrator.runMatrix(request);
    }

    @GetMapping("/runs/{runId}")
    public ExperimentRunSnapshot getRun(@PathVariable String runId) {
        return runResultStore.findExperimentRun(runId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Run not found: " + runId));
    }

    @GetMapping("/matrix-runs/{matrixRunId}")
    public MatrixRunSnapshot getMatrixRun(@PathVariable String matrixRunId) {
        return runResultStore.findMatrixRun(matrixRunId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Matrix run not found: " + matrixRunId));
    }

    @GetMapping("/stock/{productId}")
    public StockSnapshot getStock(@PathVariable Long productId) {
        StockSnapshot snapshot = stockAccessPort.findSnapshot(productId);
        if (snapshot == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found: " + productId);
        }
        return snapshot;
    }
}
