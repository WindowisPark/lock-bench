package io.lockbench.domain.port;

import io.lockbench.domain.model.StockSnapshot;

public interface StockAccessPort {
    void initialize(Long productId, int quantity);

    StockSnapshot findSnapshot(Long productId);

    boolean decreaseWithoutLock(Long productId, int quantity);

    boolean decreaseWithOptimisticLock(Long productId, int quantity, long expectedVersion);

    boolean decreaseWithPessimisticLock(Long productId, int quantity);
}
