package io.lockbench.infra.mysql;

import io.lockbench.domain.model.StockSnapshot;
import io.lockbench.domain.port.StockAccessPort;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
@Profile("mysql")
@Transactional
public class MySqlStockAccessAdapter implements StockAccessPort {

    private final StockJpaRepository stockJpaRepository;

    public MySqlStockAccessAdapter(StockJpaRepository stockJpaRepository) {
        this.stockJpaRepository = stockJpaRepository;
    }

    @Override
    public void initialize(Long productId, int quantity) {
        Optional<StockEntity> existing = stockJpaRepository.findById(productId);
        if (existing.isPresent()) {
            StockEntity entity = existing.get();
            entity.setQuantity(quantity);
            stockJpaRepository.save(entity);
            return;
        }
        stockJpaRepository.save(new StockEntity(productId, quantity));
    }

    @Override
    @Transactional(readOnly = true)
    public StockSnapshot findSnapshot(Long productId) {
        return stockJpaRepository.findById(productId)
                .map(entity -> new StockSnapshot(entity.getProductId(), entity.getQuantity(), entity.getVersion()))
                .orElse(null);
    }

    @Override
    public boolean decreaseWithoutLock(Long productId, int quantity) {
        int updated = stockJpaRepository.decrementQuantityNoLock(productId, quantity);
        return updated > 0;
    }

    @Override
    public boolean decreaseWithOptimisticLock(Long productId, int quantity, long expectedVersion) {
        try {
            int updated = stockJpaRepository.decrementQuantityOptimistic(productId, quantity, expectedVersion);
            return updated > 0;
        } catch (OptimisticLockingFailureException ex) {
            return false;
        }
    }

    @Override
    public boolean decreaseWithPessimisticLock(Long productId, int quantity) {
        Optional<StockEntity> existing = stockJpaRepository.findByIdForUpdate(productId);
        if (existing.isEmpty()) {
            return false;
        }
        StockEntity entity = existing.get();
        if (entity.getQuantity() < quantity) {
            return false;
        }
        entity.setQuantity(entity.getQuantity() - quantity);
        stockJpaRepository.save(entity);
        return true;
    }
}
