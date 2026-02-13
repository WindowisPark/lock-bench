package io.lockbench.concurrency.lock;

import io.lockbench.domain.model.StockSnapshot;
import io.lockbench.domain.port.StockAccessPort;
import org.springframework.stereotype.Component;

@Component
public class OptimisticLockStrategy implements StockLockStrategy {

    private final StockAccessPort stockAccessPort;

    public OptimisticLockStrategy(StockAccessPort stockAccessPort) {
        this.stockAccessPort = stockAccessPort;
    }

    @Override
    public LockStrategyType type() {
        return LockStrategyType.OPTIMISTIC_LOCK;
    }

    @Override
    public boolean placeOrder(Long productId, int quantity, int optimisticRetries) {
        int retries = Math.max(0, optimisticRetries);
        for (int i = 0; i <= retries; i++) {
            StockSnapshot snapshot = stockAccessPort.findSnapshot(productId);
            if (snapshot == null || snapshot.quantity() < quantity) {
                return false;
            }

            boolean updated = stockAccessPort.decreaseWithOptimisticLock(
                    productId,
                    quantity,
                    snapshot.version()
            );
            if (updated) {
                return true;
            }
        }
        return false;
    }
}
