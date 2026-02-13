package io.lockbench.concurrency.lock;

import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class StockLockStrategyFactory {

    private final Map<LockStrategyType, StockLockStrategy> strategies = new EnumMap<>(LockStrategyType.class);

    public StockLockStrategyFactory(List<StockLockStrategy> lockStrategies) {
        for (StockLockStrategy strategy : lockStrategies) {
            strategies.put(strategy.type(), strategy);
        }
    }

    public StockLockStrategy get(LockStrategyType type) {
        StockLockStrategy strategy = strategies.get(type);
        if (strategy == null) {
            throw new IllegalArgumentException("Unsupported lock strategy: " + type);
        }
        return strategy;
    }
}
