package io.lockbench.concurrency.thread;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public interface ThreadExecutionStrategy extends AutoCloseable {
    <T> CompletableFuture<T> submit(Supplier<T> task);

    @Override
    void close();
}
