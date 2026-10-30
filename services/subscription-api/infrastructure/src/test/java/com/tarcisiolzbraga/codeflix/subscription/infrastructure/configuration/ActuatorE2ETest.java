package com.tarcisiolzbraga.codeflix.subscription.infrastructure.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tarcisiolzbraga.codeflix.subscription.infrastructure.E2ETest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatusCode;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.client.RestClient;

// @E2ETest, e não @IntegrationTest, porque a porta de gerenciamento é um segundo servidor de
// verdade: a fatia sem container de servlet não tem o que medir aqui. Mora ao lado do
// SecurityE2ETest porque o que ele fixa também é regra de acesso — a das sondas.
//
// As duas portas sobem em porta aleatória: a da aplicação pelo RANDOM_PORT da anotação, a do
// actuator pelo management.server.port=0 abaixo.
@E2ETest
@TestPropertySource(properties = "management.server.port=0")
class ActuatorE2ETest {

    @LocalServerPort
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

        assertTrue(actual.contains("application=\"subscription-codeflix\""), actual);
    }

    // Sem token dá 401 nesta API, que é ambíguo: seria o mesmo se o actuator estivesse mapeado aqui
    // e apenas protegido. O 401, e não o 404, é portanto a prova possível sem credencial — e o que
    // ele mostra é que a porta da aplicação não entrega saúde a ninguém de fora.
    @Test
    void givenNoToken_whenGetHealthOnApplicationPort_thenRefuseIt() {
        final var client = clientOn(this.appPort);

        final var actual = statusOf(client, "/actuator/health");

        assertEquals(401, actual.value());
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
