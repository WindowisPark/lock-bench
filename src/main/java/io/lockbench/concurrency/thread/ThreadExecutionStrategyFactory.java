package io.lockbench.concurrency.thread;

import org.springframework.stereotype.Component;

@Component
public class ThreadExecutionStrategyFactory {

    public ThreadExecutionStrategy create(ThreadModelType threadModelType, int requestedConcurrency) {
        return switch (threadModelType) {
            case PLATFORM -> new PlatformThreadExecutionStrategy(normalizePlatformPoolSize(requestedConcurrency));
            case VIRTUAL -> new VirtualThreadExecutionStrategy();
        };
    }

    private int normalizePlatformPoolSize(int requestedConcurrency) {
        int maxPool = Runtime.getRuntime().availableProcessors() * 4;
        return Math.max(1, Math.min(requestedConcurrency, maxPool));
    }
}
