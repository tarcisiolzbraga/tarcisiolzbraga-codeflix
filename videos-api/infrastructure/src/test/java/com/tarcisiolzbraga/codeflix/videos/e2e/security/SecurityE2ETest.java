package com.tarcisiolzbraga.codeflix.videos.e2e.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.tarcisiolzbraga.codeflix.videos.infrastructure.E2ETest;
import com.tarcisiolzbraga.codeflix.videos.infrastructure.KeycloakTestToken;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.RestClient;

// Quem pode ler o catálogo, do lado de fora e com token de verdade. Os papéis vêm do Keycloak do
// container, então o caminho inteiro é exercitado: pedido do token, assinatura, validação pelo
// resource server e a decisão do @Secured.
@E2ETest
class SecurityE2ETest {

    private static final String QUERY = "{ categories { meta { total } } }";
    private static final String CLASSIFICATION = "classification";
    private static final String MUTATION =
            """
            mutation { saveCategory(input: { id: "00000024-0000-0000-0000-000000000000", name: "Filmes",
              createdAt: "2026-09-30T12:00:00Z", updatedAt: "2026-10-01T08:30:00Z" })
              { id } }""";

    @Value("${local.server.port}")
    private int port;

    @Autowired
    private KeycloakTestToken token;

    @Test
    void givenNoToken_whenReadTheCatalog_thenRefuseItAsUnauthorized() {
        final var actualResponse = read(null);

        assertEquals("UNAUTHORIZED", classificationOf(actualResponse));
    }

    // O client sem papel algum: identificou-se, e ainda assim não pode ler. É o que separa "não sei
    // quem é" de "sei quem é, e não basta".
    @Test
    void givenATokenWithoutTheRole_whenReadTheCatalog_thenRefuseItAsForbidden() {
        final var actualResponse = read(this.token.bearerOf(KeycloakTestToken.STRANGER));

        assertEquals("FORBIDDEN", classificationOf(actualResponse));
    }

    @Test
    void givenTheSubscriberRole_whenReadTheCatalog_thenServeIt() {
        final var actualResponse = read(this.token.subscriber());

        assertNull(actualResponse.get("errors"), String.valueOf(actualResponse.get("errors")));
        assertNotNull(actualResponse.get("data"));
    }

    // O administrador abre tudo, inclusive o que é do assinante.
    @Test
    void givenTheAdminRole_whenReadTheCatalog_thenServeIt() {
        final var actualResponse = read(this.token.admin());

        assertNull(actualResponse.get("errors"), String.valueOf(actualResponse.get("errors")));
        assertNotNull(actualResponse.get("data"));
    }

    // Token que não é do emissor não passa do filtro: aqui a recusa é HTTP, antes de o GraphQL
    // existir, e por isso o corpo não traz errors.
    @Test
    void givenAnInvalidToken_whenReadTheCatalog_thenRefuseItBeforeTheQueryRuns() {
        final var actualStatus = statusOf("Bearer nao-e-um-token");

        assertEquals(401, actualStatus.value());
    }

    // A porta de escrita é mais estreita que a de leitura: o assinante que lê o acervo não grava
    // nele. É aqui que essa diferença fica provada.
    @Test
    void givenTheSubscriberRole_whenCallTheExampleMutation_thenRefuseIt() {
        final var actualResponse = save(this.token.subscriber());

        assertEquals("FORBIDDEN", classificationOf(actualResponse));
    }

    @Test
    void givenTheAdminRole_whenCallTheExampleMutation_thenAcceptIt() {
        final var actualResponse = save(this.token.admin());

        assertNull(actualResponse.get("errors"), String.valueOf(actualResponse.get("errors")));
        assertNotNull(actualResponse.get("data"));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> read(final String bearer) {
        return clientWith(bearer)
                .post()
                .uri("/graphql")
                .body(Map.of("query", QUERY))
                .retrieve()
                .body(Map.class);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> save(final String bearer) {
        return clientWith(bearer)
                .post()
                .uri("/graphql")
                .body(Map.of("query", MUTATION))
                .retrieve()
                .body(Map.class);
    }

    private HttpStatusCode statusOf(final String bearer) {
        return clientWith(bearer)
                .post()
                .uri("/graphql")
                .body(Map.of("query", QUERY))
                .exchange((request, response) -> response.getStatusCode());
    }

    @SuppressWarnings("unchecked")
    private String classificationOf(final Map<String, Object> response) {
        final var errors = (List<Map<String, Object>>) response.get("errors");
        assertNotNull(errors, "a resposta não trouxe errors: " + response);
        final var extensions = (Map<String, Object>) errors.getFirst().get("extensions");
        assertNotNull(extensions, "o erro não trouxe extensions: " + errors);
        return String.valueOf(extensions.get(CLASSIFICATION));
    }

    private RestClient clientWith(final String bearer) {
        final var builder = RestClient.builder().baseUrl("http://localhost:%d/api".formatted(this.port));
        if (bearer != null) {
            builder.defaultHeader(HttpHeaders.AUTHORIZATION, bearer);
        }
        return builder.build();
    }
}
