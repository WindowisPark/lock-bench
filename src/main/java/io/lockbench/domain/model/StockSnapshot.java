package io.lockbench.domain.model;

public record StockSnapshot(
        Long productId,
        int quantity,
        long version
) {
}
