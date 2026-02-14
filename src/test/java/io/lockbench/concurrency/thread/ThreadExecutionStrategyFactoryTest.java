package io.lockbench.concurrency.thread;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ThreadExecutionStrategyFactoryTest {

    @Test
    void usesFixedConcurrencyPerThreadModel() {
        ThreadExecutionStrategyFactory factory = new ThreadExecutionStrategyFactory(32, 96);

        assertEquals(32, factory.effectiveConcurrency(ThreadModelType.PLATFORM));
        assertEquals(96, factory.effectiveConcurrency(ThreadModelType.VIRTUAL));
    }
}
