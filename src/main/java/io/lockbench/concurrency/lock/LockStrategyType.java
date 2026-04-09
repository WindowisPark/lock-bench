package io.lockbench.concurrency.lock;

public enum LockStrategyType {
    NO_LOCK,
    OPTIMISTIC_LOCK,
    PESSIMISTIC_LOCK,
    REDIS_DISTRIBUTED_LOCK,
    REDISSON_PUB_SUB_LOCK,
    REDISSON_FAIR_LOCK
}
