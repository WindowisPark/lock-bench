package io.lockbench.concurrency.lock;

import io.lockbench.domain.model.OrderFailureReason;
import io.lockbench.domain.model.OrderResult;
import io.lockbench.domain.model.StockSnapshot;
import io.lockbench.domain.port.StockAccessPort;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;

@Component
public class OptimisticLockStrategy implements StockLockStrategy {

    private static final int MAX_OPTIMISTIC_RETRIES = 5;
    private static final long BASE_BACKOFF_MILLIS = 2L;
    private static final long MAX_BACKOFF_MILLIS = 32L;

    private final StockAccessPort stockAccessPort;

    public OptimisticLockStrategy(StockAccessPort stockAccessPort) {
        this.stockAccessPort = stockAccessPort;
    }

    @Override
    public LockStrategyType type() {
        return LockStrategyType.OPTIMISTIC_LOCK;
    }

    @Override
    public OrderResult placeOrder(Long productId, int quantity, int optimisticRetries) {
        if (quantity <= 0) {
            return OrderResult.fail(OrderFailureReason.INVALID_QUANTITY);
        }

        int retries = Math.min(Math.max(0, optimisticRetries), MAX_OPTIMISTIC_RETRIES);
        for (int attempt = 0; attempt <= retries; attempt++) {
            StockSnapshot snapshot = stockAccessPort.findSnapshot(productId);
            if (snapshot == null) {
                return OrderResult.fail(OrderFailureReason.PRODUCT_NOT_FOUND);
            }
            if (snapshot.quantity() < quantity) {
                return OrderResult.fail(OrderFailureReason.OUT_OF_STOCK);
            }

            boolean updated = stockAccessPort.decreaseWithOptimisticLock(
                    productId,
                    quantity,
                    snapshot.version()
            );
            if (updated) {
                return OrderResult.ok();
            }
            if (attempt < retries) {
                backoff(attempt);
            }
        }
        return OrderResult.fail(OrderFailureReason.VERSION_CONFLICT);
    }

    private void backoff(int attempt) {
        long exponential = BASE_BACKOFF_MILLIS << Math.min(attempt, 4);
        long baseDelay = Math.min(MAX_BACKOFF_MILLIS, exponential);
        long jitter = ThreadLocalRandom.current().nextLong(baseDelay + 1);
        long sleepMillis = baseDelay + jitter;
        try {
            Thread.sleep(sleepMillis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

