package io.lockbench.concurrency.lock;

import io.lockbench.domain.model.OrderFailureReason;
import io.lockbench.domain.model.OrderResult;
import io.lockbench.domain.port.StockAccessPort;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@ConditionalOnProperty(name = "lockbench.redisson-lock.enabled", havingValue = "true")
public class RedissonFairLockStrategy implements StockLockStrategy {

    private final RedissonClient redissonClient;
    private final StockAccessPort stockAccessPort;
    private final boolean enabled;
    private final long waitTimeMillis;
    private final long leaseTimeMillis;

    public RedissonFairLockStrategy(
            RedissonClient redissonClient,
            StockAccessPort stockAccessPort,
            @Value("${lockbench.redisson-lock.enabled:false}") boolean enabled,
            @Value("${lockbench.redisson-lock.wait-time-millis:10000}") long waitTimeMillis,
            @Value("${lockbench.redisson-lock.lease-time-millis:8000}") long leaseTimeMillis
    ) {
        this.redissonClient = redissonClient;
        this.stockAccessPort = stockAccessPort;
        this.enabled = enabled;
        this.waitTimeMillis = waitTimeMillis;
        this.leaseTimeMillis = leaseTimeMillis;
    }

    @Override
    public LockStrategyType type() {
        return LockStrategyType.REDISSON_FAIR_LOCK;
    }

    @Override
    public OrderResult placeOrder(Long productId, int quantity, int optimisticRetries, long holdMillis) {
        if (!enabled) {
            throw new IllegalStateException("Redisson fair lock is disabled. Set lockbench.redisson-lock.enabled=true.");
        }
        if (quantity <= 0) {
            return OrderResult.fail(OrderFailureReason.INVALID_QUANTITY);
        }

        String key = "lock:stock:" + productId;
        RLock lock = redissonClient.getFairLock(key);

        boolean acquired;
        try {
            acquired = lock.tryLock(waitTimeMillis, leaseTimeMillis, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return OrderResult.fail(OrderFailureReason.LOCK_TIMEOUT);
        }

        if (!acquired) {
            return OrderResult.fail(OrderFailureReason.LOCK_TIMEOUT);
        }

        try {
            boolean updated = stockAccessPort.decreaseWithoutLock(productId, quantity);
            if (holdMillis > 0) {
                try {
                    Thread.sleep(holdMillis);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            if (updated) {
                return OrderResult.ok();
            }
            return stockAccessPort.findSnapshot(productId) == null
                    ? OrderResult.fail(OrderFailureReason.PRODUCT_NOT_FOUND)
                    : OrderResult.fail(OrderFailureReason.OUT_OF_STOCK);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
