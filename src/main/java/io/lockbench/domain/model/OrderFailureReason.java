package io.lockbench.domain.model;

public enum OrderFailureReason {
    OUT_OF_STOCK,
    VERSION_CONFLICT,
    LOCK_TIMEOUT,
    INVALID_QUANTITY,
    PRODUCT_NOT_FOUND
}

