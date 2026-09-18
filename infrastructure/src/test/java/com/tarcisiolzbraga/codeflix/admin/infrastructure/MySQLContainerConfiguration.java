package com.tarcisiolzbraga.codeflix.admin.infrastructure;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
public class MySQLContainerConfiguration {

    // A mesma versão do docker compose: o schema é validado contra o banco de verdade.
    private static final DockerImageName MYSQL_IMAGE = DockerImageName.parse("mysql:8.4");

    @Bean
    @ServiceConnection
    MySQLContainer<?> mysqlContainer() {
        return new MySQLContainer<>(MYSQL_IMAGE).withDatabaseName("adm_videos_test");
    }
}
