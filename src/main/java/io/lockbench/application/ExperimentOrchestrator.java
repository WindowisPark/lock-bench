package io.lockbench.application;

import io.lockbench.api.dto.ExperimentRequest;
import io.lockbench.api.dto.ExperimentResponse;
import io.lockbench.concurrency.lock.StockLockStrategy;
import io.lockbench.concurrency.lock.StockLockStrategyFactory;
import io.lockbench.concurrency.thread.ThreadExecutionStrategy;
import io.lockbench.concurrency.thread.ThreadExecutionStrategyFactory;
import io.lockbench.domain.model.OrderResult;
import io.lockbench.domain.port.StockAccessPort;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
public class ExperimentOrchestrator {

    private final StockLockStrategyFactory lockStrategyFactory;
    private final ThreadExecutionStrategyFactory threadExecutionStrategyFactory;
    private final StockAccessPort stockAccessPort;

    public ExperimentOrchestrator(
            StockLockStrategyFactory lockStrategyFactory,
            ThreadExecutionStrategyFactory threadExecutionStrategyFactory,
            StockAccessPort stockAccessPort
    ) {
        this.lockStrategyFactory = lockStrategyFactory;
        this.threadExecutionStrategyFactory = threadExecutionStrategyFactory;
        this.stockAccessPort = stockAccessPort;
    }

    public ExperimentResponse run(ExperimentRequest request) {
        String runId = UUID.randomUUID().toString();
        StockLockStrategy stockLockStrategy = lockStrategyFactory.get(request.lockStrategy());
        int effectiveConcurrency = threadExecutionStrategyFactory.effectiveConcurrency(request.threadModel());

        stockAccessPort.initialize(request.productId(), request.initialStock());

        Instant start = Instant.now();
        List<Long> latencyNanos = new ArrayList<>(request.totalRequests());
        Map<String, Integer> failureBreakdown = new HashMap<>();
        int successCount = 0;
        int failureCount = 0;

        try (ThreadExecutionStrategy threadExecutionStrategy =
                     threadExecutionStrategyFactory.create(request.threadModel())) {

            List<CompletableFuture<OrderResult>> futures = new ArrayList<>(request.totalRequests());
            for (int i = 0; i < request.totalRequests(); i++) {
                CompletableFuture<OrderResult> future = threadExecutionStrategy.submit(() -> {
                    long begin = System.nanoTime();
                    OrderResult result = stockLockStrategy.placeOrder(
                            request.productId(),
                            request.quantity(),
                            request.optimisticRetries()
                    );
                    long end = System.nanoTime();
                    synchronized (latencyNanos) {
                        latencyNanos.add(end - begin);
                    }
                    return result;
                });
                futures.add(future);
            }

            for (CompletableFuture<OrderResult> future : futures) {
                OrderResult result = future.join();
                if (result.success()) {
                    successCount++;
                } else {
                    failureCount++;
                    String reason = result.failureReason() == null ? "UNKNOWN" : result.failureReason().name();
                    failureBreakdown.merge(reason, 1, Integer::sum);
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
                effectiveConcurrency,
                request.totalRequests(),
                successCount,
                failureCount,
                Map.copyOf(failureBreakdown),
                elapsedMillis,
                throughputPerSec,
                latency.p50Millis(),
                latency.p95Millis(),
                latency.p99Millis()
        );
    }
}

