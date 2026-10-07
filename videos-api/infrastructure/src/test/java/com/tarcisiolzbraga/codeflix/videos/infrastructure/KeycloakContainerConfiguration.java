package com.tarcisiolzbraga.codeflix.videos.infrastructure;

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

// Um emissor de verdade para os testes, como na admin-api: o token é pedido, assinado e validado de
// ponta a ponta, em vez de forjado.
//
// O realm é o da raiz do monorepo, o mesmo que o docker compose importa — não há realm de teste
// próprio. Enquanto os dois projetos eram repositórios separados havia um aqui, mínimo, porque
// apontar para fora do repositório faria a suíte depender de um clone vizinho; o preço era os dois
// poderem divergir no nome dos papéis. No monorepo o arquivo é um só, e a divergência deixa de ser
// possível.
//
// O segredo dos clients vem com um marcador no arquivo versionado, do mesmo jeito que o compose
// encontra; aqui a troca é por um valor fixo de teste.
//
// Estático e fora do ciclo de vida do Spring, como os outros containers: um Keycloak por contexto
// não se paga.
@TestConfiguration(proxyBeanMethods = false)
public class KeycloakContainerConfiguration {

    private static final DockerImageName IMAGE = DockerImageName.parse("quay.io/keycloak/keycloak:26.8.0");
    private static final int HTTP_PORT = 8080;
    private static final String REALM = "codeflix";
    private static final String SECRET_PLACEHOLDER = "__KEYCLOAK_CLIENT_SECRET__";

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
        final var path = Path.of(System.getProperty("codeflix.rootDir"), "..", ".keycloak", "realm.json");
        try {
            return Files.readString(path).replace(SECRET_PLACEHOLDER, KeycloakTestToken.SECRET);
        } catch (final IOException exception) {
            throw new UncheckedIOException("could not read the realm at " + path, exception);
        }
    }
}
