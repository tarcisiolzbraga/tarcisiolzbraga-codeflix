package com.tarcisiolzbraga.codeflix.subscription.infrastructure;

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

// A mesma imagem e o mesmo realm do docker compose: a jornada apresenta um token de verdade, emitido
// por um emissor de verdade, e não um forjado. O realm é lido do arquivo versionado da raiz do
// monorepo, então teste e desenvolvimento não podem divergir nas roles nem no nome do client.
//
// Os dois marcadores do arquivo versionado — o segredo dos clients e a senha do
// `operador-de-teste` — são trocados aqui por valores fixos de teste. O do operador não é usado
// por teste nenhum (aquele usuário existe para entrar no back-office pelo browser), mas deixar o
// marcador sem trocar gravaria o texto dele como senha, e o realm dos testes passaria a diferir
// do realm do compose de um jeito invisível.
//
// O container sobe uma vez e é reaproveitado entre as classes pelo cache de contexto do Spring.
@TestConfiguration(proxyBeanMethods = false)
public class KeycloakContainerConfiguration {

    private static final DockerImageName KEYCLOAK_IMAGE = DockerImageName.parse("quay.io/keycloak/keycloak:26.8.0");
    private static final int HTTP_PORT = 8080;

    private static final String REALM = "codeflix";
    // O client que tem a role CODEFLIX_ADMIN, que é quem administra plano.
    private static final String CLIENT_ID = "admin-codeflix";
    // O client com as roles do realm-management, com que o serviço administra os usuários.
    private static final String ADMIN_CLIENT_ID = "subscription-api-codeflix";
    private static final String ACCOUNTS_GROUP = "accounts";
    private static final String SUBSCRIBERS_GROUP = "subscribers";
    private static final String CLIENT_SECRET = "segredo-de-teste";
    private static final String SECRET_PLACEHOLDER = "__KEYCLOAK_CLIENT_SECRET__";
    private static final String OPERATOR_PASSWORD_PLACEHOLDER = "__OPERATOR_TEST_PASSWORD__";
    private static final String OPERATOR_PASSWORD = "operador-de-teste";

    // Estático e fora do ciclo de vida do Spring: como bean, subiria um Keycloak por contexto.
    private static final GenericContainer<?> CONTAINER = startedContainer();

    private static GenericContainer<?> startedContainer() {
        final GenericContainer<?> container = new GenericContainer<>(KEYCLOAK_IMAGE);
        container.withEnv("KC_BOOTSTRAP_ADMIN_USERNAME", "admin");
        container.withEnv("KC_BOOTSTRAP_ADMIN_PASSWORD", "admin");
        container.withCopyToContainer(Transferable.of(realmDefinition()), "/opt/keycloak/data/import/realm.json");
        container.withCommand("start-dev", "--import-realm");
        container.withExposedPorts(HTTP_PORT);
        container.waitingFor(Wait.forLogMessage(".*Listening on.*", 1));
        container.start();
        return container;
    }

    // O emissor e a credencial de administração juntos: todo o apontamento para o Keycloak de teste
    // sai de um lugar só, como no application.yml.
    @Bean
    DynamicPropertyRegistrar keycloakProperties() {
        return registry -> {
            registry.add("keycloak.issuer-uri", KeycloakContainerConfiguration::issuerUri);
            registry.add("keycloak.admin.client-id", () -> ADMIN_CLIENT_ID);
            registry.add("keycloak.admin.client-secret", () -> CLIENT_SECRET);
            registry.add("keycloak.admin.accounts-group", () -> ACCOUNTS_GROUP);
            registry.add("keycloak.admin.subscribers-group", () -> SUBSCRIBERS_GROUP);
        };
    }

    @Bean
    KeycloakTestToken keycloakTestToken() {
        return new KeycloakTestToken(issuerUri(), CLIENT_ID, CLIENT_SECRET);
    }

    // Token de pessoa, para as rotas `/me`: o client que o emite é criado no container, não no realm
    // versionado — o porquê está no KeycloakTestUserToken.
    @Bean
    KeycloakTestUserToken keycloakTestUserToken() {
        return new KeycloakTestUserToken(baseUrl(), REALM);
    }

    private static String issuerUri() {
        return "%s/realms/%s".formatted(baseUrl(), REALM);
    }

    private static String baseUrl() {
        return "http://%s:%d".formatted(CONTAINER.getHost(), CONTAINER.getMappedPort(HTTP_PORT));
    }

    // O arquivo versionado traz um marcador no lugar do segredo, do mesmo jeito que o docker compose
    // encontra: aqui a troca é por um valor fixo de teste.
    private static String realmDefinition() {
        final var path =
                Path.of(System.getProperty("codeflix.rootDir"), "..", "..", "provisioning", "keycloak", "realm.json");
        try {
            return Files.readString(path)
                    .replace(SECRET_PLACEHOLDER, CLIENT_SECRET)
                    .replace(OPERATOR_PASSWORD_PLACEHOLDER, OPERATOR_PASSWORD);
        } catch (final IOException exception) {
            throw new UncheckedIOException("could not read the realm at " + path, exception);
        }
    }
}
