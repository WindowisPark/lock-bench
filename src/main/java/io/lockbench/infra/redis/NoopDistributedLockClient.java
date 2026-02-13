package io.lockbench.infra.redis;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@ConditionalOnMissingBean(RedisDistributedLockClient.class)
public class NoopDistributedLockClient implements DistributedLockClient {

    @Override
    public boolean tryLock(String key, String token, Duration ttl) {
        return false;
    }

    @Override
    public void unlock(String key, String token) {
    }
}
