package io.lockbench.concurrency.lock;

import io.lockbench.domain.model.OrderFailureReason;
import io.lockbench.domain.model.OrderResult;
import io.lockbench.domain.model.StockSnapshot;
import io.lockbench.domain.port.StockAccessPort;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class OptimisticLockStrategyTest {

    @Test
    void returnsInvalidQuantityWhenQuantityIsNonPositive() {
        OptimisticLockStrategy strategy = new OptimisticLockStrategy(new AlwaysConflictPort());

        OrderResult result = strategy.placeOrder(1L, 0, 3);

        assertFalse(result.success());
        assertEquals(OrderFailureReason.INVALID_QUANTITY, result.failureReason());
    }

    @Test
    void returnsProductNotFoundWhenSnapshotIsMissing() {
        OptimisticLockStrategy strategy = new OptimisticLockStrategy(new MissingProductPort());

        OrderResult result = strategy.placeOrder(1L, 1, 3);

        assertFalse(result.success());
        assertEquals(OrderFailureReason.PRODUCT_NOT_FOUND, result.failureReason());
    }

    @Test
    void returnsOutOfStockWhenQuantityIsInsufficient() {
        OptimisticLockStrategy strategy = new OptimisticLockStrategy(new OutOfStockPort());

        OrderResult result = strategy.placeOrder(1L, 2, 3);

        assertFalse(result.success());
        assertEquals(OrderFailureReason.OUT_OF_STOCK, result.failureReason());
    }

    @Test
    void capsRetriesAtFiveAndReturnsVersionConflict() {
        AlwaysConflictPort port = new AlwaysConflictPort();
        OptimisticLockStrategy strategy = new OptimisticLockStrategy(port);

        OrderResult result = strategy.placeOrder(1L, 1, 100);

        assertFalse(result.success());
        assertEquals(OrderFailureReason.VERSION_CONFLICT, result.failureReason());
        assertEquals(6, port.decreaseCalls.get());
    }

    private static final class MissingProductPort implements StockAccessPort {
        @Override
        public void initialize(Long productId, int quantity) {
        }

        @Override
        public StockSnapshot findSnapshot(Long productId) {
            return null;
        }

        @Override
        public boolean decreaseWithoutLock(Long productId, int quantity) {
            return false;
        }

        @Override
        public boolean decreaseWithOptimisticLock(Long productId, int quantity, long expectedVersion) {
            return false;
        }

        @Override
        public boolean decreaseWithPessimisticLock(Long productId, int quantity) {
            return false;
        }
    }

    private static final class OutOfStockPort implements StockAccessPort {
        @Override
        public void initialize(Long productId, int quantity) {
        }

        @Override
        public StockSnapshot findSnapshot(Long productId) {
            return new StockSnapshot(productId, 1, 0);
        }

        @Override
        public boolean decreaseWithoutLock(Long productId, int quantity) {
            return false;
        }

        @Override
        public boolean decreaseWithOptimisticLock(Long productId, int quantity, long expectedVersion) {
            return false;
        }

        @Override
        public boolean decreaseWithPessimisticLock(Long productId, int quantity) {
            return false;
        }
    }

    private static final class AlwaysConflictPort implements StockAccessPort {
        private final AtomicInteger decreaseCalls = new AtomicInteger();

        @Override
        public void initialize(Long productId, int quantity) {
        }

        @Override
        public StockSnapshot findSnapshot(Long productId) {
            return new StockSnapshot(productId, 100, 7);
        }

        @Override
        public boolean decreaseWithoutLock(Long productId, int quantity) {
            return false;
        }

        @Override
        public boolean decreaseWithOptimisticLock(Long productId, int quantity, long expectedVersion) {
            decreaseCalls.incrementAndGet();
            return false;
        }

        @Override
        public boolean decreaseWithPessimisticLock(Long productId, int quantity) {
            return false;
        }
    }
}

