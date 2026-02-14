package io.lockbench.concurrency.lock;

import io.lockbench.domain.model.OrderFailureReason;
import io.lockbench.domain.model.OrderResult;
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
    public OrderResult placeOrder(Long productId, int quantity, int optimisticRetries) {
        if (quantity <= 0) {
            return OrderResult.fail(OrderFailureReason.INVALID_QUANTITY);
        }

        boolean updated = stockAccessPort.decreaseWithoutLock(productId, quantity);
        if (updated) {
            return OrderResult.ok();
        }
        return stockAccessPort.findSnapshot(productId) == null
                ? OrderResult.fail(OrderFailureReason.PRODUCT_NOT_FOUND)
                : OrderResult.fail(OrderFailureReason.OUT_OF_STOCK);
    }
}

