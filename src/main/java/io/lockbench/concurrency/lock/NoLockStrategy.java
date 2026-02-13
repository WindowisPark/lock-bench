package io.lockbench.concurrency.lock;

import io.lockbench.domain.port.StockAccessPort;
import org.springframework.stereotype.Component;

@Component
public class NoLockStrategy implements StockLockStrategy {

    private final StockAccessPort stockAccessPort;

    public NoLockStrategy(StockAccessPort stockAccessPort) {
        this.stockAccessPort = stockAccessPort;
    }

    @Override
    public LockStrategyType type() {
        return LockStrategyType.NO_LOCK;
    }

    @Override
    public boolean placeOrder(Long productId, int quantity, int optimisticRetries) {
        return stockAccessPort.decreaseWithoutLock(productId, quantity);
    }
}
