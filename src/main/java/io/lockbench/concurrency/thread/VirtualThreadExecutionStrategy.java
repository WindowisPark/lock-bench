package io.lockbench.concurrency.thread;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

public class VirtualThreadExecutionStrategy implements ThreadExecutionStrategy {

    private final ExecutorService executorService;

    public VirtualThreadExecutionStrategy() {
        this.executorService = Executors.newVirtualThreadPerTaskExecutor();
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
