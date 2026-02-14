package io.lockbench.concurrency.thread;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ThreadExecutionStrategyFactory {

    private final int platformConcurrency;
    private final int virtualConcurrency;

    public ThreadExecutionStrategyFactory(
            @Value("${lockbench.thread-fixed.platform-concurrency:200}") int platformConcurrency,
            @Value("${lockbench.thread-fixed.virtual-concurrency:200}") int virtualConcurrency
    ) {
        this.platformConcurrency = Math.max(1, platformConcurrency);
        this.virtualConcurrency = Math.max(1, virtualConcurrency);
    }

    public ThreadExecutionStrategy create(ThreadModelType threadModelType) {
        return switch (threadModelType) {
            case PLATFORM -> new PlatformThreadExecutionStrategy(platformConcurrency);
            case VIRTUAL -> new VirtualThreadExecutionStrategy(virtualConcurrency);
        };
    }

    public int effectiveConcurrency(ThreadModelType threadModelType) {
        return switch (threadModelType) {
            case PLATFORM -> platformConcurrency;
            case VIRTUAL -> virtualConcurrency;
        };
    }
}
