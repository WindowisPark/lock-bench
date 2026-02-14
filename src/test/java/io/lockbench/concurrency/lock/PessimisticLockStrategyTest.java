package io.lockbench.concurrency.lock;

import io.lockbench.domain.model.OrderFailureReason;
import io.lockbench.domain.model.OrderResult;
import io.lockbench.domain.model.StockSnapshot;
import io.lockbench.infra.memory.InMemoryStockAccessAdapter;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class PessimisticLockStrategyTest {

    @Test
    void keepsStockInvariantUnderConcurrentOrders() {
        InMemoryStockAccessAdapter adapter = new InMemoryStockAccessAdapter();
        adapter.initialize(1L, 100);
        PessimisticLockStrategy strategy = new PessimisticLockStrategy(adapter);

        ExecutorService pool = Executors.newFixedThreadPool(16);
        try {
            List<CompletableFuture<OrderResult>> futures = new ArrayList<>();
            for (int i = 0; i < 300; i++) {
                futures.add(CompletableFuture.supplyAsync(() -> strategy.placeOrder(1L, 1, 0), pool));
            }

            int success = 0;
            int outOfStock = 0;
            for (CompletableFuture<OrderResult> future : futures) {
                OrderResult result = future.join();
                if (result.success()) {
                    success++;
                } else if (result.failureReason() == OrderFailureReason.OUT_OF_STOCK) {
                    outOfStock++;
                }
            }

            StockSnapshot snapshot = adapter.findSnapshot(1L);
            assertNotNull(snapshot);
            assertEquals(100, success);
            assertEquals(200, outOfStock);
            assertEquals(0, snapshot.quantity());
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void returnsProductNotFoundWhenProductIsMissing() {
        InMemoryStockAccessAdapter adapter = new InMemoryStockAccessAdapter();
        PessimisticLockStrategy strategy = new PessimisticLockStrategy(adapter);

        OrderResult result = strategy.placeOrder(999L, 1, 0);

        assertFalse(result.success());
        assertEquals(OrderFailureReason.PRODUCT_NOT_FOUND, result.failureReason());
    }
}

