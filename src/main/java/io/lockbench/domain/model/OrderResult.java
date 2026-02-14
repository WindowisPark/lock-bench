package io.lockbench.domain.model;

public record OrderResult(
        boolean success,
        OrderFailureReason failureReason
) {
    public static OrderResult ok() {
        return new OrderResult(true, null);
    }

    public static OrderResult fail(OrderFailureReason reason) {
        return new OrderResult(false, reason);
    }
}

