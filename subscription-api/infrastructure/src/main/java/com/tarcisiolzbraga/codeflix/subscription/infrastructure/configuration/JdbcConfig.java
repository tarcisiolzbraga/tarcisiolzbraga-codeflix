package com.tarcisiolzbraga.codeflix.subscription.infrastructure.configuration;

import com.tarcisiolzbraga.codeflix.subscription.infrastructure.jdbc.DatabaseClient;
import com.tarcisiolzbraga.codeflix.subscription.infrastructure.jdbc.JdbcClientAdapter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.simple.JdbcClient;

@Configuration(proxyBeanMethods = false)
public class JdbcConfig {

    @Bean
    DatabaseClient databaseClient(final JdbcClient jdbcClient) {
        return new JdbcClientAdapter(jdbcClient);
    }
}
