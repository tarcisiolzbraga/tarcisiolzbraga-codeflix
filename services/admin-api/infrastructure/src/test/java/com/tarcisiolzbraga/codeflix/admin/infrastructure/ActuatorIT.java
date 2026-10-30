package com.tarcisiolzbraga.codeflix.admin.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.client.RestClient;

// As duas portas sobem em porta aleatória: a da aplicação pelo RANDOM_PORT, a do actuator pelo
// management.server.port=0 abaixo.
@IntegrationTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = "management.server.port=0")
class ActuatorIT {

    @Value("${local.server.port}")
    private int appPort;

    @Value("${local.management.port}")
    private int managementPort;

    @Autowired
    private KeycloakTestToken token;

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

        assertTrue(actual.contains("application=\"admin-codeflix\""), actual);
    }

    // Com token de administrador, e não sem token: sem ele a regra geral responderia 401, que é
    // ambíguo — seria o mesmo que se o actuator estivesse mapeado aqui e apenas protegido. O 404
    // com credencial que abre tudo é o que prova que não há actuator nesta porta.
    @Test
    void givenAnAdminToken_whenGetHealthOnApplicationPort_thenDoNotServeIt() {
        final var client = RestClient.builder()
                .baseUrl("http://localhost:" + this.appPort)
                .defaultHeader(HttpHeaders.AUTHORIZATION, this.token.bearer())
                .build();

        final var actual = statusOf(client, "/actuator/health");

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
