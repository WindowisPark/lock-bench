package io.lockbench.application;

import io.lockbench.api.dto.ExperimentResponse;
import io.lockbench.api.dto.ExperimentRunSnapshot;
import io.lockbench.api.dto.MatrixRunResponse;
import io.lockbench.api.dto.MatrixRunSnapshot;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Deque;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

@Component
public class InMemoryRunResultStore implements RunResultStore {

    private final int maxSize;
    private final ConcurrentHashMap<String, ExperimentRunSnapshot> experimentRuns = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, MatrixRunSnapshot> matrixRuns = new ConcurrentHashMap<>();
    private final Deque<String> experimentOrder = new ConcurrentLinkedDeque<>();
    private final Deque<String> matrixOrder = new ConcurrentLinkedDeque<>();

    public InMemoryRunResultStore(@Value("${lockbench.run-store.max-size:500}") int maxSize) {
        this.maxSize = Math.max(1, maxSize);
    }

    @Override
    public void saveExperimentResult(ExperimentResponse response) {
        ExperimentRunSnapshot snapshot = new ExperimentRunSnapshot(
                response.runId(),
                Instant.now(),
                response
        );
        putWithEviction(response.runId(), snapshot, experimentRuns, experimentOrder);
    }

    @Override
    public Optional<ExperimentRunSnapshot> findExperimentRun(String runId) {
        return Optional.ofNullable(experimentRuns.get(runId));
    }

    @Override
    public void saveMatrixRunResult(MatrixRunResponse response) {
        MatrixRunSnapshot snapshot = new MatrixRunSnapshot(
                response.matrixRunId(),
                Instant.now(),
                response
        );
        putWithEviction(response.matrixRunId(), snapshot, matrixRuns, matrixOrder);
    }

    @Override
    public Optional<MatrixRunSnapshot> findMatrixRun(String matrixRunId) {
        return Optional.ofNullable(matrixRuns.get(matrixRunId));
    }

    private <T> void putWithEviction(
            String id,
            T snapshot,
            ConcurrentHashMap<String, T> store,
            Deque<String> insertionOrder
    ) {
        synchronized (insertionOrder) {
            store.put(id, snapshot);
            insertionOrder.remove(id);
            insertionOrder.addLast(id);
            while (insertionOrder.size() > maxSize) {
                String evictId = insertionOrder.pollFirst();
                if (evictId != null) {
                    store.remove(evictId);
                }
            }
        }
    }
}
