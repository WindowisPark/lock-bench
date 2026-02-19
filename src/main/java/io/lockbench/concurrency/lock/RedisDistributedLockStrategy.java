package io.lockbench.concurrency.lock;

import io.lockbench.domain.model.OrderFailureReason;
import io.lockbench.domain.model.OrderResult;
import io.lockbench.domain.port.StockAccessPort;
import io.lockbench.infra.redis.DistributedLockClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class RedisDistributedLockStrategy implements StockLockStrategy {

    private final DistributedLockClient distributedLockClient;
    private final StockAccessPort stockAccessPort;
    private final boolean redisLockEnabled;
    private final long ttlMillis;
    private final int maxRetries;
    private final long baseBackoffMillis;
    private final long maxBackoffMillis;

    public RedisDistributedLockStrategy(
            DistributedLockClient distributedLockClient,
            StockAccessPort stockAccessPort,
            @Value("${lockbench.redis-lock.enabled:false}") boolean redisLockEnabled,
            @Value("${lockbench.redis-lock.ttl-millis:2000}") long ttlMillis,
            @Value("${lockbench.redis-lock.max-retries:3}") int maxRetries,
            @Value("${lockbench.redis-lock.base-backoff-millis:5}") long baseBackoffMillis,
            @Value("${lockbench.redis-lock.max-backoff-millis:50}") long maxBackoffMillis
    ) {
        this.distributedLockClient = distributedLockClient;
        this.stockAccessPort = stockAccessPort;
        this.redisLockEnabled = redisLockEnabled;
        this.ttlMillis = ttlMillis;
        this.maxRetries = Math.max(0, maxRetries);
        this.baseBackoffMillis = Math.max(0L, baseBackoffMillis);
        this.maxBackoffMillis = Math.max(this.baseBackoffMillis, maxBackoffMillis);
    }

    @Override
    public LockStrategyType type() {
        return LockStrategyType.REDIS_DISTRIBUTED_LOCK;
    }

    @Override
    public OrderResult placeOrder(Long productId, int quantity, int optimisticRetries) {
        if (!redisLockEnabled) {
            throw new IllegalStateException("Redis distributed lock is disabled. Set lockbench.redis-lock.enabled=true.");
        }
        if (quantity <= 0) {
            return OrderResult.fail(OrderFailureReason.INVALID_QUANTITY);
        }

        String key = "lock:stock:" + productId;
        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            String token = UUID.randomUUID().toString();
            boolean acquired = distributedLockClient.tryLock(key, token, Duration.ofMillis(ttlMillis));
            if (!acquired) {
                if (attempt < maxRetries) {
                    backoff(attempt);
                    continue;
                }
                return OrderResult.fail(OrderFailureReason.LOCK_TIMEOUT);
            }

            try {
                boolean updated = stockAccessPort.decreaseWithoutLock(productId, quantity);
                if (updated) {
                    return OrderResult.ok();
                }
                return stockAccessPort.findSnapshot(productId) == null
                        ? OrderResult.fail(OrderFailureReason.PRODUCT_NOT_FOUND)
                        : OrderResult.fail(OrderFailureReason.OUT_OF_STOCK);
            } finally {
                distributedLockClient.unlock(key, token);
            }
        }
        return OrderResult.fail(OrderFailureReason.LOCK_TIMEOUT);
    }

    private void backoff(int attempt) {
        if (baseBackoffMillis <= 0) {
            return;
        }
        long exponential = baseBackoffMillis << Math.min(attempt, 10);
        long baseDelay = Math.min(maxBackoffMillis, exponential);
        long jitter = ThreadLocalRandom.current().nextLong(baseDelay + 1);
        long sleepMillis = baseDelay + jitter;
        try {
            Thread.sleep(sleepMillis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

