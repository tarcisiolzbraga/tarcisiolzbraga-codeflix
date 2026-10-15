package com.tarcisiolzbraga.codeflix.subscription.infrastructure;

import java.util.Map;
import java.util.UUID;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

// A mesma imagem do compose do CDC: o consumidor de eventos fala com um Kafka de verdade.
//
// Sobe para todo contexto de teste, e não só para quem afirma sobre evento: o @KafkaListener é um
// bean da aplicação, e sem broker alcançável ele derruba a subida do contexto inteiro — foi assim
// que descobri, com as jornadas vermelhas e um ConfigException de bootstrap-servers vazio.
@TestConfiguration(proxyBeanMethods = false)
public class KafkaContainerConfiguration {

    private static final DockerImageName KAFKA_IMAGE = DockerImageName.parse("apache/kafka:4.3.1");

    // Estático e fora do ciclo de vida do Spring: como bean, subiria um broker por contexto.
    private static final KafkaContainer CONTAINER = startedContainer();

    private static KafkaContainer startedContainer() {
        final var container = new KafkaContainer(KAFKA_IMAGE);
        container.start();
        return container;
    }

    // Grupo de consumidor sorteado por contexto, lição emprestada da videos-api: o broker é um só e
    // é reaproveitado entre as classes, então com grupo fixo o consumidor de um contexto antigo
    // ainda vivo comeria a mensagem publicada por outro — e o teste falharia sem explicação.
    //
    // O auto-offset-reset segue earliest, como em produção, e é ele que impede a mensagem de se
    // perder quando a partição é atribuída depois de o teste já ter publicado.
    @Bean
    DynamicPropertyRegistrar kafkaProperties() {
        final var group = "test-" + UUID.randomUUID();
        return registry -> {
            registry.add("spring.kafka.bootstrap-servers", CONTAINER::getBootstrapServers);
            registry.add("kafka.consumers.events.group-id", () -> group);
        };
    }

    // O KafkaTemplate que o Boot declara vem com curingas no tipo, então não casa com uma injeção
    // de KafkaTemplate<String, String>. Este é só para o teste publicar a mensagem de CDC, que é
    // texto dos dois lados, como o consumidor espera.
    @Bean
    KafkaTemplate<String, String> testKafkaTemplate() {
        final var settings = Map.<String, Object>of(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, CONTAINER.getBootstrapServers(),
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        return new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(settings));
    }
}
