package io.lockbench.concurrency.thread;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.function.Supplier;

public class VirtualThreadExecutionStrategy implements ThreadExecutionStrategy {

    private final ExecutorService executorService;
    private final Semaphore semaphore;

    public VirtualThreadExecutionStrategy(int maxConcurrency) {
        this.executorService = Executors.newVirtualThreadPerTaskExecutor();
        this.semaphore = new Semaphore(Math.max(1, maxConcurrency));
    }

    @Override
    public <T> CompletableFuture<T> submit(Supplier<T> task) {
        return CompletableFuture.supplyAsync(() -> {
            semaphore.acquireUninterruptibly();
            try {
                return task.get();
            } finally {
                semaphore.release();
            }
        }, executorService);
    }

    @Override
    public void close() {
        executorService.shutdown();
    }
}
