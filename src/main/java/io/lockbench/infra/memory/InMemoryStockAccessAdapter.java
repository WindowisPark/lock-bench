package io.lockbench.infra.memory;

import io.lockbench.domain.model.StockSnapshot;
import io.lockbench.domain.port.StockAccessPort;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

@Component
public class InMemoryStockAccessAdapter implements StockAccessPort {

    private final Map<Long, AtomicReference<StockState>> states = new ConcurrentHashMap<>();
    private final Map<Long, ReentrantLock> locks = new ConcurrentHashMap<>();

    @Override
    public void initialize(Long productId, int quantity) {
        states.put(productId, new AtomicReference<>(new StockState(quantity, 0)));
        locks.put(productId, new ReentrantLock());
    }

    @Override
    public StockSnapshot findSnapshot(Long productId) {
        AtomicReference<StockState> reference = states.get(productId);
        if (reference == null) {
            return null;
        }
        StockState state = reference.get();
        return new StockSnapshot(productId, state.quantity(), state.version());
    }

    @Override
    public boolean decreaseWithoutLock(Long productId, int quantity) {
        AtomicReference<StockState> reference = states.get(productId);
        if (reference == null) {
            return false;
        }

        StockState current = reference.get();
        if (current.quantity() < quantity) {
            return false;
        }
        StockState next = new StockState(current.quantity() - quantity, current.version() + 1);
        reference.set(next);
        return true;
    }

    @Override
    public boolean decreaseWithOptimisticLock(Long productId, int quantity, long expectedVersion) {
        AtomicReference<StockState> reference = states.get(productId);
        if (reference == null) {
            return false;
        }

        StockState current = reference.get();
        if (current.version() != expectedVersion || current.quantity() < quantity) {
            return false;
        }

        StockState next = new StockState(current.quantity() - quantity, current.version() + 1);
        return reference.compareAndSet(current, next);
    }

    @Override
    public boolean decreaseWithPessimisticLock(Long productId, int quantity) {
        ReentrantLock lock = locks.computeIfAbsent(productId, key -> new ReentrantLock());
        lock.lock();
        try {
            AtomicReference<StockState> reference = states.get(productId);
            if (reference == null) {
                return false;
            }

            StockState current = reference.get();
            if (current.quantity() < quantity) {
                return false;
            }

            StockState next = new StockState(current.quantity() - quantity, current.version() + 1);
            reference.set(next);
            return true;
        } finally {
            lock.unlock();
        }
    }

    private record StockState(int quantity, long version) {
    }
}
