package io.lockbench.concurrency.lock;

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
    public boolean placeOrder(Long productId, int quantity, int optimisticRetries) {
        String key = "lock:stock:" + productId;
        String token = UUID.randomUUID().toString();
        boolean acquired = distributedLockClient.tryLock(key, token, Duration.ofSeconds(2));
        if (!acquired) {
            return false;
        }

        try {
            return stockAccessPort.decreaseWithoutLock(productId, quantity);
        } finally {
            distributedLockClient.unlock(key, token);
        }
    }
}
