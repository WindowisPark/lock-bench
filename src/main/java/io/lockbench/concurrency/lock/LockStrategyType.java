package io.lockbench.concurrency.lock;

public enum LockStrategyType {
    NO_LOCK,
    OPTIMISTIC_LOCK,
    PESSIMISTIC_LOCK,
    REDIS_DISTRIBUTED_LOCK
}
