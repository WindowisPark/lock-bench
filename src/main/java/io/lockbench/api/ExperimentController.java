package io.lockbench.api;

import io.lockbench.api.dto.ExperimentRequest;
import io.lockbench.api.dto.ExperimentResponse;
import io.lockbench.application.ExperimentOrchestrator;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/experiments")
public class ExperimentController {

    private final ExperimentOrchestrator experimentOrchestrator;

    public ExperimentController(ExperimentOrchestrator experimentOrchestrator) {
        this.experimentOrchestrator = experimentOrchestrator;
    }

    @PostMapping("/run")
    public ExperimentResponse run(@Valid @RequestBody ExperimentRequest request) {
        return experimentOrchestrator.run(request);
    }
}
