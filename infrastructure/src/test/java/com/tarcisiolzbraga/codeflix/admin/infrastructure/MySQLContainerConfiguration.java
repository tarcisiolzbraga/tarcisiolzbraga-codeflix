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

    // O container fica em variável e é devolvido: quem fecha é o Spring, no fim do
    // contexto. Encadear o withDatabaseName() faria o compilador do Eclipse acusar
    // vazamento de recurso, por ver um Closeable criado e nunca atribuído.
    @Bean
    @ServiceConnection
    MySQLContainer<?> mysqlContainer() {
        final MySQLContainer<?> container = new MySQLContainer<>(MYSQL_IMAGE);
        container.withDatabaseName("adm_videos_test");
        return container;
    }
}
