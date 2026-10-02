package com.tarcisiolzbraga.codeflix.videos.infrastructure;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.utility.DockerImageName;

// A mesma versão do docker compose: o mapeamento dos índices é exercitado contra o servidor de
// verdade, não contra um substituto em memória.
//
// O container é estático e sobe uma vez por execução, fora do ciclo de vida do Spring. Como bean ele
// subiria de novo a cada contexto da suíte, e um Elasticsearch por contexto não se paga.
@TestConfiguration(proxyBeanMethods = false)
public class ElasticsearchContainerConfiguration {

    private static final DockerImageName IMAGE =
            DockerImageName.parse("docker.elastic.co/elasticsearch/elasticsearch:9.5.4");

    private static final ElasticsearchContainer CONTAINER = startedContainer();

    @Bean
    DynamicPropertyRegistrar elasticsearchProperties() {
        return registry -> registry.add("spring.elasticsearch.uris", () -> "http://" + CONTAINER.getHttpHostAddress());
    }

    private static ElasticsearchContainer startedContainer() {
        final var container = new ElasticsearchContainer(IMAGE);
        // Desenvolvimento e teste: sem TLS nem senha, como no compose.
        container.withEnv("xpack.security.enabled", "false");
        container.withEnv("discovery.type", "single-node");
        container.withEnv("ES_JAVA_OPTS", "-Xms512m -Xmx512m");
        container.start();
        return container;
    }
}
