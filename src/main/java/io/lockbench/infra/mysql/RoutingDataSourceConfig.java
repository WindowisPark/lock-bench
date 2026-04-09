package io.lockbench.infra.mysql;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import javax.sql.DataSource;
import java.util.Map;

@Configuration
@Profile("mysql")
@ConditionalOnProperty(name = "lockbench.routing-datasource.enabled", havingValue = "true")
public class RoutingDataSourceConfig {

    @Value("${lockbench.routing-datasource.write-pool-size:10}")
    private int writePoolSize;

    @Value("${lockbench.routing-datasource.read-pool-size:10}")
    private int readPoolSize;

    @Bean
    @Primary
    public DataSource routingDataSource(DataSourceProperties properties) {
        DataSource writeDs = createPool(properties, "write-pool", writePoolSize);
        DataSource readDs = createPool(properties, "read-pool", readPoolSize);

        ReadWriteRoutingDataSource routing = new ReadWriteRoutingDataSource();
        routing.setTargetDataSources(Map.of(
                ReadWriteRoutingDataSource.DataSourceType.WRITE, writeDs,
                ReadWriteRoutingDataSource.DataSourceType.READ, readDs
        ));
        routing.setDefaultTargetDataSource(writeDs);
        routing.afterPropertiesSet();
        return routing;
    }

    private HikariDataSource createPool(DataSourceProperties props, String poolName, int poolSize) {
        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl(props.getUrl());
        ds.setUsername(props.getUsername());
        ds.setPassword(props.getPassword());
        ds.setPoolName(poolName);
        ds.setMaximumPoolSize(poolSize);
        ds.setMinimumIdle(poolSize);
        return ds;
    }
}
