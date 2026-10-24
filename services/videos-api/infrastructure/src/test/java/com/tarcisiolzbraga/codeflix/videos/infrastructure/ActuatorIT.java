package com.tarcisiolzbraga.codeflix.videos.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpStatusCode;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.client.RestClient;

// As duas portas sobem em porta aleatória: a da aplicação pelo RANDOM_PORT, a do actuator pelo
// management.server.port=0 abaixo.
//
// Sem token em nenhum caso, e aqui isso não é descuido: a regra geral desta API é `permitAll` e
// quem autoriza é o @Secured de cada consulta do GraphQL, então um 404 já é resposta sem
// ambiguidade — diferente da admin-api, onde o mesmo caminho sem token daria 401.
@IntegrationTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = "management.server.port=0")
class ActuatorIT {

    @Value("${local.server.port}")
    private int appPort;

    @Value("${local.management.port}")
    private int managementPort;

    @Test
    void givenNoToken_whenGetReadinessOnManagementPort_thenAnswerUp() {
        final var client = clientOn(this.managementPort);

        final var actual = client.get().uri("/actuator/health/readiness").retrieve().body(String.class);

        assertTrue(actual.contains("\"status\":\"UP\""), actual);
    }

    @Test
    void givenNoToken_whenGetLivenessOnManagementPort_thenAnswerUp() {
        final var client = clientOn(this.managementPort);

        final var actual = client.get().uri("/actuator/health/liveness").retrieve().body(String.class);

        assertTrue(actual.contains("\"status\":\"UP\""), actual);
    }

    @Test
    void givenNoToken_whenScrapeMetrics_thenAnswerTaggedByApplication() {
        final var client = clientOn(this.managementPort);

        final var actual = client.get().uri("/actuator/prometheus").retrieve().body(String.class);

        assertTrue(actual.contains("application=\"videos-api-codeflix\""), actual);
    }

    // O `context-path: /api` é do servidor da aplicação e não atravessa para o de gerenciamento.
    // Fixar isso importa porque a sonda escrita com o prefixo falharia em produção, não aqui.
    @Test
    void givenTheApiContextPath_whenUseItOnManagementPort_thenDoNotServeIt() {
        final var client = clientOn(this.managementPort);

        final var actual = statusOf(client, "/api/actuator/health");

        assertEquals(404, actual.value());
    }

    @Test
    void givenNoToken_whenGetHealthOnApplicationPort_thenDoNotServeIt() {
        final var client = clientOn(this.appPort);

        final var actual = statusOf(client, "/api/actuator/health");

        assertEquals(404, actual.value());
    }

    private RestClient clientOn(final int port) {
        return RestClient.builder().baseUrl("http://localhost:" + port).build();
    }

    private HttpStatusCode statusOf(final RestClient client, final String uri) {
        return client.get()
                .uri(uri)
                .exchange((request, response) -> response.getStatusCode(), false);
    }
}
