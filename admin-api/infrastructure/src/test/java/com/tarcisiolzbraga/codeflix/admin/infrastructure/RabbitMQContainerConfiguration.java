package com.tarcisiolzbraga.codeflix.admin.infrastructure;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.utility.DockerImageName;

// A mesma imagem do docker compose, como o MySQL e o Garage: o teste fala com um broker de verdade.
// As propriedades são registradas à mão, e não por @ServiceConnection, porque o YAML comum declara
// spring.rabbitmq.* sem valor padrão de propósito: sem sobrescrever, a ligação do RabbitProperties
// falharia nos perfis homolog e production, onde a variável de ambiente não existe.
@TestConfiguration(proxyBeanMethods = false)
public class RabbitMQContainerConfiguration {

    private static final DockerImageName RABBITMQ_IMAGE = DockerImageName.parse("rabbitmq:4.2-management")
            .asCompatibleSubstituteFor("rabbitmq");

    // Estático e fora do ciclo de vida do Spring: como bean, subiria um broker por contexto.
    private static final RabbitMQContainer CONTAINER = startedContainer();

    private static RabbitMQContainer startedContainer() {
        final var container = new RabbitMQContainer(RABBITMQ_IMAGE);
        container.start();
        return container;
    }

    @Bean
    DynamicPropertyRegistrar rabbitMQProperties() {
        return registry -> {
            registry.add("spring.rabbitmq.host", CONTAINER::getHost);
            registry.add("spring.rabbitmq.port", CONTAINER::getAmqpPort);
            registry.add("spring.rabbitmq.username", CONTAINER::getAdminUsername);
            registry.add("spring.rabbitmq.password", CONTAINER::getAdminPassword);
            // Uma hora: o relay não dispara sozinho no meio dos testes. Quem quiser exercitá-lo
            // chama deliverPending() à mão, e assim cada teste controla o próprio tempo.
            registry.add("amqp.outbox.poll-interval", () -> 3600000);
            registry.add("amqp.outbox.cleanup-interval", () -> 3600000);
        };
    }
}
