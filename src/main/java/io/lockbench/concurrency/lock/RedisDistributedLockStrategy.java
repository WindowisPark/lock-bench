package io.lockbench.concurrency.lock;

import io.lockbench.domain.model.OrderFailureReason;
import io.lockbench.domain.model.OrderResult;
import io.lockbench.domain.port.StockAccessPort;
import io.lockbench.infra.redis.DistributedLockClient;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;

@Component
public class RedisDistributedLockStrategy implements StockLockStrategy {

    private final DistributedLockClient distributedLockClient;
    private final StockAccessPort stockAccessPort;

    public RedisDistributedLockStrategy(
            DistributedLockClient distributedLockClient,
            StockAccessPort stockAccessPort
    ) {
        this.distributedLockClient = distributedLockClient;
        this.stockAccessPort = stockAccessPort;
    }

    @Override
    public LockStrategyType type() {
        return LockStrategyType.REDIS_DISTRIBUTED_LOCK;
    }

    @Override
    public OrderResult placeOrder(Long productId, int quantity, int optimisticRetries) {
        if (quantity <= 0) {
            return OrderResult.fail(OrderFailureReason.INVALID_QUANTITY);
        }

        String key = "lock:stock:" + productId;
        String token = UUID.randomUUID().toString();
        boolean acquired = distributedLockClient.tryLock(key, token, Duration.ofSeconds(2));
        if (!acquired) {
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
}

