package com.devflow.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * One container for the whole suite. Declared as a static singleton rather than a
 * per-class @Container so the ~2s startup is paid once instead of per test class.
 */
@TestConfiguration(proxyBeanMethods = false)
public class PostgresContainerConfig {

    private static final PostgreSQLContainer CONTAINER = new PostgreSQLContainer("postgres:17-alpine")
            .withDatabaseName("devflow_test")
            .withUsername("devflow")
            .withPassword("devflow")
            .withReuse(true);

    static {
        CONTAINER.start();
    }

    @Bean
    @ServiceConnection
    public PostgreSQLContainer postgresContainer() {
        return CONTAINER;
    }
}
