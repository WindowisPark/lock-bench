package io.lockbench.concurrency.lock;

import io.lockbench.domain.model.OrderResult;

public interface StockLockStrategy {
    LockStrategyType type();

    OrderResult placeOrder(Long productId, int quantity, int optimisticRetries);
}

