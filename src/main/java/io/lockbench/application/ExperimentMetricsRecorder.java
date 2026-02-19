package io.lockbench.application;

import io.lockbench.api.dto.ExperimentResponse;
import io.lockbench.api.dto.MatrixRunResponse;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class ExperimentMetricsRecorder {

    private final MeterRegistry meterRegistry;

    public ExperimentMetricsRecorder(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void recordExperimentRun(ExperimentResponse response) {
        Tags tags = Tags.of(
                "thread_model", response.threadModel(),
                "lock_strategy", response.lockStrategy()
        );

        Counter.builder("lockbench.experiment.run.count")
                .tags(tags)
                .register(meterRegistry)
                .increment();

        Counter.builder("lockbench.experiment.request.success.count")
                .tags(tags)
                .register(meterRegistry)
                .increment(response.successCount());

        Counter.builder("lockbench.experiment.request.failure.count")
                .tags(tags)
                .register(meterRegistry)
                .increment(response.failureCount());

        Timer.builder("lockbench.experiment.elapsed")
                .description("Experiment total elapsed time")
                .tags(tags)
                .register(meterRegistry)
                .record(Duration.ofNanos(response.elapsedNanos()));

        DistributionSummary.builder("lockbench.experiment.throughput")
                .description("Experiment throughput in requests/sec")
                .baseUnit("req_per_sec")
                .tags(tags)
                .register(meterRegistry)
                .record(response.throughputPerSec());
    }

    public void recordMatrixRun(MatrixRunResponse response) {
        Counter.builder("lockbench.matrix.run.count")
                .register(meterRegistry)
                .increment();

        Counter.builder("lockbench.matrix.scenario.success.count")
                .register(meterRegistry)
                .increment(response.successScenarios());

        Counter.builder("lockbench.matrix.scenario.failure.count")
                .register(meterRegistry)
                .increment(response.failedScenarios());
    }
}
