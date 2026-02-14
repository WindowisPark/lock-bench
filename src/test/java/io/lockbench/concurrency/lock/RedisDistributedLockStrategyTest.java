package io.lockbench.concurrency.lock;

import io.lockbench.domain.model.OrderFailureReason;
import io.lockbench.domain.model.OrderResult;
import io.lockbench.domain.model.StockSnapshot;
import io.lockbench.domain.port.StockAccessPort;
import io.lockbench.infra.redis.DistributedLockClient;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RedisDistributedLockStrategyTest {

    @Test
    void throwsWhenRedisLockIsDisabled() {
        RedisDistributedLockStrategy strategy = new RedisDistributedLockStrategy(
                new AlwaysLockClient(),
                new SimpleStockPort(),
                false
        );

        assertThrows(IllegalStateException.class, () -> strategy.placeOrder(1L, 1, 0));
    }

    @Test
    void returnsLockTimeoutWhenLockNotAcquired() {
        RedisDistributedLockStrategy strategy = new RedisDistributedLockStrategy(
                new NeverLockClient(),
                new SimpleStockPort(),
                true
        );

        OrderResult result = strategy.placeOrder(1L, 1, 0);

        assertEquals(OrderFailureReason.LOCK_TIMEOUT, result.failureReason());
    }

    private static final class AlwaysLockClient implements DistributedLockClient {
        @Override
        public boolean tryLock(String key, String token, Duration ttl) {
            return true;
        }

        @Override
        public void unlock(String key, String token) {
        }
    }

    private static final class NeverLockClient implements DistributedLockClient {
        @Override
        public boolean tryLock(String key, String token, Duration ttl) {
            return false;
        }

        @Override
        public void unlock(String key, String token) {
        }
    }

    private static final class SimpleStockPort implements StockAccessPort {
        @Override
        public void initialize(Long productId, int quantity) {
        }

        @Override
        public StockSnapshot findSnapshot(Long productId) {
            return new StockSnapshot(productId, 10, 1);
        }

        @Override
        public boolean decreaseWithoutLock(Long productId, int quantity) {
            return true;
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
}
