package io.lockbench.infra.redis;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "lockbench.redisson-lock.enabled", havingValue = "true")
public class RedissonConfig {

    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient(
            @Value("${spring.data.redis.host:localhost}") String host,
            @Value("${spring.data.redis.port:6379}") int port,
            @Value("${spring.data.redis.password:}") String password
    ) {
        Config config = new Config();
        String address = "redis://" + host + ":" + port;
        var serverConfig = config.useSingleServer()
                .setAddress(address)
                .setConnectionMinimumIdleSize(10)
                .setConnectionPoolSize(64);
        if (password != null && !password.isBlank()) {
            serverConfig.setPassword(password);
        }
        return Redisson.create(config);
    }
}
