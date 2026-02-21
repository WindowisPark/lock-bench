package io.lockbench.concurrency.lock;

import io.lockbench.domain.model.OrderFailureReason;
import io.lockbench.domain.model.OrderResult;
import io.lockbench.domain.port.StockAccessPort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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
    @Transactional
    public OrderResult placeOrder(Long productId, int quantity, int optimisticRetries, long holdMillis) {
        if (quantity <= 0) {
            return OrderResult.fail(OrderFailureReason.INVALID_QUANTITY);
        }

        // @Transactional keeps the DB connection + row lock open until this method returns.
        // holdMillis simulates slow business logic inside the critical section.
        boolean updated = stockAccessPort.decreaseWithPessimisticLock(productId, quantity);
        if (updated) {
            if (holdMillis > 0) {
                try { Thread.sleep(holdMillis); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            }
            return OrderResult.ok();
        }
        return stockAccessPort.findSnapshot(productId) == null
                ? OrderResult.fail(OrderFailureReason.PRODUCT_NOT_FOUND)
                : OrderResult.fail(OrderFailureReason.OUT_OF_STOCK);
    }
}

