package com.tarcisiolzbraga.codeflix.admin.infrastructure;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.utility.DockerImageName;

// A mesma imagem e o mesmo realm do docker compose: o teste fala com um emissor de verdade, e não
// com um token forjado. O realm é lido do arquivo versionado, então teste e desenvolvimento não
// podem divergir nas roles nem no nome do client.
//
// O container sobe uma vez e é reaproveitado entre as classes pelo cache de contexto do Spring.
@TestConfiguration(proxyBeanMethods = false)
public class KeycloakContainerConfiguration {

    private static final DockerImageName KEYCLOAK_IMAGE = DockerImageName.parse("quay.io/keycloak/keycloak:26.8.0");
    private static final int HTTP_PORT = 8080;

    private static final String REALM = "codeflix";
    private static final String CLIENT_ID = "admin-codeflix";
    private static final String CLIENT_SECRET = "segredo-de-teste";
    private static final String SECRET_PLACEHOLDER = "__KEYCLOAK_CLIENT_SECRET__";

    // Estático e fora do ciclo de vida do Spring: como bean, subiria um Keycloak por contexto.
    private static final GenericContainer<?> CONTAINER = startedContainer();

    private static GenericContainer<?> startedContainer() {
        final GenericContainer<?> container = new GenericContainer<>(KEYCLOAK_IMAGE);
        container.withEnv("KC_BOOTSTRAP_ADMIN_USERNAME", "admin");
        container.withEnv("KC_BOOTSTRAP_ADMIN_PASSWORD", "admin");
        container.withCopyToContainer(
                Transferable.of(realmDefinition()), "/opt/keycloak/data/import/realm.json");
        container.withCommand("start-dev", "--import-realm");
        container.withExposedPorts(HTTP_PORT);
        container.waitingFor(Wait.forLogMessage(".*Listening on.*", 1));
        container.start();
        return container;
    }

    @Bean
    DynamicPropertyRegistrar keycloakIssuerProperty() {
        return registry -> registry.add("keycloak.issuer-uri", KeycloakContainerConfiguration::issuerUri);
    }

    @Bean
    KeycloakTestToken keycloakTestToken() {
        return new KeycloakTestToken(issuerUri(), CLIENT_ID, CLIENT_SECRET);
    }

    private static String issuerUri() {
        return "http://%s:%d/realms/%s".formatted(CONTAINER.getHost(), CONTAINER.getMappedPort(HTTP_PORT), REALM);
    }

    // O arquivo versionado traz um marcador no lugar do segredo, do mesmo jeito que o docker compose
    // encontra: aqui a troca é por um valor fixo de teste.
    private static String realmDefinition() {
        final var path = Path.of(System.getProperty("codeflix.rootDir"), "..", "provisioning", "keycloak", "realm.json");
        try {
            return Files.readString(path).replace(SECRET_PLACEHOLDER, CLIENT_SECRET);
        } catch (final IOException exception) {
            throw new UncheckedIOException("could not read the realm at " + path, exception);
        }
    }
}
