package io.lockbench.concurrency.thread;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

public class PlatformThreadExecutionStrategy implements ThreadExecutionStrategy {

    private final ExecutorService executorService;

    public PlatformThreadExecutionStrategy(int poolSize) {
        this.executorService = Executors.newFixedThreadPool(poolSize);
    }

    @Override
    public <T> CompletableFuture<T> submit(Supplier<T> task) {
        return CompletableFuture.supplyAsync(task, executorService);
    }

    @Override
    public void close() {
        executorService.shutdown();
    }
}
