package com.tarcisiolzbraga.codeflix.subscription.infrastructure.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.tarcisiolzbraga.codeflix.subscription.infrastructure.E2ETest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestClient;

// O que só o Tomcat de verdade mostra. A fatia @WebMvcTest não serve para isto: o MockMvc devolve o
// 404 do DispatcherServlet direto, sem encaminhar para /error, então o despacho de erro — que é
// justamente o que estava quebrado — nunca acontece lá e o teste passaria com e sem a correção.
@E2ETest
class SecurityE2ETest {

    @LocalServerPort
    private int port;

    // A regressão: o Spring Security filtra todo tipo de despacho. Com o anyRequest() protegido, o
    // encaminhamento para /error entrava anônimo e era recusado, e o cliente recebia 401 no lugar do
    // status de verdade. Esta rota é liberada e ainda não tem handler, então o certo é 404.
    @Test
    void givenNoToken_whenCallsAPermittedRouteWithNoHandler_thenGetsNotFoundAndNotUnauthorized() {
        final var client = clientOf("/accounts/sign-up");

        final var actual = client.post().retrieve().toBodilessEntity();

        assertEquals(HttpStatus.NOT_FOUND, actual.getStatusCode());
    }

    @Test
    void givenNoToken_whenCallsAProtectedRoute_thenIsUnauthorized() {
        final var client = clientOf("/plans");

        final var actual = client.get().retrieve().toBodilessEntity();

        assertEquals(HttpStatus.UNAUTHORIZED, actual.getStatusCode());
    }

    @Test
    void givenNoToken_whenCallsTheApiDocumentation_thenItIsOpen() {
        final var client = clientOf("/v3/api-docs");

        final var actual = client.get().retrieve().toBodilessEntity();

        assertEquals(HttpStatus.OK, actual.getStatusCode());
    }

    // Sem o onStatus, o RestClient lança em 4xx e 5xx: aqui o status É a asserção, então o erro tem
    // de voltar como resposta.
    private RestClient clientOf(final String path) {
        return RestClient.builder()
                .baseUrl("http://localhost:%d%s".formatted(this.port, path))
                .defaultStatusHandler(status -> true, (request, response) -> {})
                .build();
    }
}
