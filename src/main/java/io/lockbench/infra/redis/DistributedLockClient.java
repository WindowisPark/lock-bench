package io.lockbench.infra.redis;

import java.time.Duration;

public interface DistributedLockClient {
    boolean tryLock(String key, String token, Duration ttl);

    void unlock(String key, String token);
}
