package io.lockbench.concurrency.lock;

import io.lockbench.domain.model.OrderFailureReason;
import io.lockbench.domain.model.OrderResult;
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
    public OrderResult placeOrder(Long productId, int quantity, int optimisticRetries) {
        if (quantity <= 0) {
            return OrderResult.fail(OrderFailureReason.INVALID_QUANTITY);
        }

        // Pessimistic strategy is responsible only for lock-protected critical section.
        // Transaction boundary is intentionally owned by application/service layer.
        boolean updated = stockAccessPort.decreaseWithPessimisticLock(productId, quantity);
        if (updated) {
            return OrderResult.ok();
        }
        return stockAccessPort.findSnapshot(productId) == null
                ? OrderResult.fail(OrderFailureReason.PRODUCT_NOT_FOUND)
                : OrderResult.fail(OrderFailureReason.OUT_OF_STOCK);
    }
}

