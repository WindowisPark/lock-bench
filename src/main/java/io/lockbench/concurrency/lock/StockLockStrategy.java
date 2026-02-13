package io.lockbench.concurrency.lock;

public interface StockLockStrategy {
    LockStrategyType type();

    boolean placeOrder(Long productId, int quantity, int optimisticRetries);
}
