package io.lockbench.application;

import io.lockbench.api.dto.ExperimentRequest;
import io.lockbench.api.dto.ExperimentResponse;
import io.lockbench.concurrency.lock.StockLockStrategy;
import io.lockbench.concurrency.lock.StockLockStrategyFactory;
import io.lockbench.concurrency.thread.ThreadExecutionStrategy;
import io.lockbench.concurrency.thread.ThreadExecutionStrategyFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
public class ExperimentOrchestrator {

    private final StockLockStrategyFactory lockStrategyFactory;
    private final ThreadExecutionStrategyFactory threadExecutionStrategyFactory;

    public ExperimentOrchestrator(
            StockLockStrategyFactory lockStrategyFactory,
            ThreadExecutionStrategyFactory threadExecutionStrategyFactory
    ) {
        this.lockStrategyFactory = lockStrategyFactory;
        this.threadExecutionStrategyFactory = threadExecutionStrategyFactory;
    }

    public ExperimentResponse run(ExperimentRequest request) {
        String runId = UUID.randomUUID().toString();
        StockLockStrategy stockLockStrategy = lockStrategyFactory.get(request.lockStrategy());

        Instant start = Instant.now();
        List<Long> latencyNanos = new ArrayList<>(request.totalRequests());
        int successCount = 0;
        int failureCount = 0;

        try (ThreadExecutionStrategy threadExecutionStrategy =
                     threadExecutionStrategyFactory.create(request.threadModel(), request.concurrency())) {

            List<CompletableFuture<Boolean>> futures = new ArrayList<>(request.totalRequests());
            for (int i = 0; i < request.totalRequests(); i++) {
                CompletableFuture<Boolean> future = threadExecutionStrategy.submit(() -> {
                    long begin = System.nanoTime();
                    boolean success = stockLockStrategy.placeOrder(
                            request.productId(),
                            request.quantity(),
                            request.optimisticRetries()
                    );
                    long end = System.nanoTime();
                    synchronized (latencyNanos) {
                        latencyNanos.add(end - begin);
                    }
                    return success;
                });
                futures.add(future);
            }

            for (CompletableFuture<Boolean> future : futures) {
                if (future.join()) {
                    successCount++;
                } else {
                    failureCount++;
                }
            }
        }

        long elapsedMillis = Duration.between(start, Instant.now()).toMillis();
        LatencySummary latency = LatencySummary.of(latencyNanos);
        double throughputPerSec = elapsedMillis == 0 ? successCount : (successCount * 1000.0) / elapsedMillis;

        return new ExperimentResponse(
                runId,
                request.threadModel().name(),
                request.lockStrategy().name(),
                request.totalRequests(),
                successCount,
                failureCount,
                elapsedMillis,
                throughputPerSec,
                latency.p50Millis(),
                latency.p95Millis(),
                latency.p99Millis()
        );
    }
}
