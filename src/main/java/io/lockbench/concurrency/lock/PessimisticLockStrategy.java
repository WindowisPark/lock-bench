package io.lockbench.concurrency.lock;

import io.lockbench.domain.port.StockAccessPort;
import org.springframework.stereotype.Component;

@Component
public class PessimisticLockStrategy implements StockLockStrategy {

    private final StockAccessPort stockAccessPort;

    public PessimisticLockStrategy(StockAccessPort stockAccessPort) {
        this.stockAccessPort = stockAccessPort;
    }

    @Override
    public LockStrategyType type() {
        return LockStrategyType.PESSIMISTIC_LOCK;
    }

    @Override
    public boolean placeOrder(Long productId, int quantity, int optimisticRetries) {
        return stockAccessPort.decreaseWithPessimisticLock(productId, quantity);
    }
}
