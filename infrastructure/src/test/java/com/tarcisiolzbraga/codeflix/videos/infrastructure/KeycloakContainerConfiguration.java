package com.tarcisiolzbraga.codeflix.videos.infrastructure;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.utility.DockerImageName;

// Um emissor de verdade para os testes, como no admin-codeflix: o token é pedido, assinado e
// validado de ponta a ponta, em vez de forjado.
//
// O realm aqui é próprio e mínimo, e não o do admin-codeflix: aquele arquivo vive no outro
// repositório, e apontar para fora daqui faria a suíte depender de um clone vizinho. O preço é que
// os dois podem divergir no nome dos papéis — por isso o nome CODEFLIX_SUBSCRIBER aparece em um
// lugar só do código, na classe Roles, e é o que este realm concede.
//
// Estático e fora do ciclo de vida do Spring, como os outros containers: um Keycloak por contexto
// não se paga.
@TestConfiguration(proxyBeanMethods = false)
public class KeycloakContainerConfiguration {

    private static final DockerImageName IMAGE = DockerImageName.parse("quay.io/keycloak/keycloak:26.8.0");
    private static final int HTTP_PORT = 8080;
    private static final String REALM = "codeflix";

    private static final GenericContainer<?> CONTAINER = startedContainer();

    @Bean
    DynamicPropertyRegistrar keycloakIssuerProperty() {
        return registry -> registry.add("keycloak.issuer-uri", KeycloakContainerConfiguration::issuerUri);
    }

    @Bean
    KeycloakTestToken keycloakTestToken() {
        return new KeycloakTestToken(issuerUri());
    }

    private static String issuerUri() {
        return "http://%s:%d/realms/%s".formatted(CONTAINER.getHost(), CONTAINER.getMappedPort(HTTP_PORT), REALM);
    }

    private static GenericContainer<?> startedContainer() {
        final GenericContainer<?> container = new GenericContainer<>(IMAGE);
        container.withEnv("KC_BOOTSTRAP_ADMIN_USERNAME", "admin");
        container.withEnv("KC_BOOTSTRAP_ADMIN_PASSWORD", "admin");
        container.withCopyToContainer(Transferable.of(realmDefinition()), "/opt/keycloak/data/import/realm.json");
        container.withCommand("start-dev", "--import-realm");
        container.withExposedPorts(HTTP_PORT);
        container.waitingFor(Wait.forLogMessage(".*Listening on.*", 1));
        container.start();
        return container;
    }

    private static String realmDefinition() {
        try {
            return new String(
                    new ClassPathResource("keycloak/realm.json").getInputStream().readAllBytes(),
                    StandardCharsets.UTF_8);
        } catch (final IOException exception) {
            throw new UncheckedIOException("could not read the test realm", exception);
        }
    }
}
