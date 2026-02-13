package io.lockbench;

import io.lockbench.domain.port.StockAccessPort;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class LockBenchApplication {

    public static void main(String[] args) {
        SpringApplication.run(LockBenchApplication.class, args);
    }

    @Bean
    ApplicationRunner bootstrapStock(StockAccessPort stockAccessPort) {
        return new ApplicationRunner() {
            @Override
            public void run(ApplicationArguments args) {
                stockAccessPort.initialize(1L, 10_000);
            }
        };
    }
}
