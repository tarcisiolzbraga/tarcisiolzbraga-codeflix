package com.tarcisiolzbraga.codeflix.admin.infrastructure;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

// A mesma versão do docker compose: o schema é validado contra o banco de verdade.
//
// O container é estático e sobe uma vez por execução, fora do ciclo de vida do Spring. Como bean ele
// subiria de novo a cada contexto, e a suíte tem onze contextos distintos — eram onze bancos para
// nada. Quem o encerra é o Ryuk do Testcontainers, no fim da JVM.
@TestConfiguration(proxyBeanMethods = false)
public class MySQLContainerConfiguration {

    private static final DockerImageName MYSQL_IMAGE = DockerImageName.parse("mysql:8.4");

    private static final MySQLContainer<?> CONTAINER = startedContainer();

    @Bean
    DynamicPropertyRegistrar mysqlProperties() {
        return registry -> {
            registry.add("spring.datasource.url", CONTAINER::getJdbcUrl);
            registry.add("spring.datasource.username", CONTAINER::getUsername);
            registry.add("spring.datasource.password", CONTAINER::getPassword);
        };
    }

    private static MySQLContainer<?> startedContainer() {
        final MySQLContainer<?> container = new MySQLContainer<>(MYSQL_IMAGE);
        container.withDatabaseName("adm_videos_test");
        container.start();
        return container;
    }
}
