package com.tarcisiolzbraga.codeflix.admin.e2e.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.tarcisiolzbraga.codeflix.admin.infrastructure.E2ETest;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.KeycloakTestToken;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

// O único lugar que prova a autenticação de ponta a ponta: token emitido por um Keycloak de
// verdade, atravessando o Tomcat e o filtro, contra a API inteira. Os outros E2E apenas levam o
// token; aqui o assunto é ele.
@E2ETest
class SecurityE2ETest {

    private static final String CATEGORIES_PATH = "/categories";
    private static final String VIDEOS_PATH = "/videos";
    private static final String RESTRICTED_CLIENT = "categories-codeflix";

    // A origem do front no perfil development, que é o que vale nos testes.
    private static final String FRONT_ORIGIN = "http://localhost:3000";

    // O back-office roda na 3001, e a origem dele faltava na lista: o painel dizia "não foi possível
    // falar com a admin-api" com a API no ar, porque o preflight voltava 403 e o browser escondia a
    // resposta. Afirmar só a 3000 era afirmar metade do que esta API serve.
    private static final String BACK_OFFICE_ORIGIN = "http://localhost:3001";

    @Value("${local.server.port}")
    private int port;

    @Autowired
    private KeycloakTestToken token;

    @Test
    void givenNoToken_whenCallTheApi_thenReturnUnauthorized() {
        final var client = clientWithout();

        final var actualException = assertThrows(
                HttpClientErrorException.class, () -> client.get().uri(VIDEOS_PATH).retrieve().toBodilessEntity());

        assertEquals(HttpStatus.UNAUTHORIZED, actualException.getStatusCode());
    }

    @Test
    void givenAGarbageToken_whenCallTheApi_thenReturnUnauthorized() {
        final var client = clientWith("Bearer nao-e-um-token");

        final var actualException = assertThrows(
                HttpClientErrorException.class, () -> client.get().uri(VIDEOS_PATH).retrieve().toBodilessEntity());

        assertEquals(HttpStatus.UNAUTHORIZED, actualException.getStatusCode());
    }

    @Test
    void givenATokenWithoutTheRole_whenCallTheApi_thenReturnForbidden() {
        final var client = clientWith(this.token.bearerOf(RESTRICTED_CLIENT));

        final var actualException = assertThrows(
                HttpClientErrorException.class, () -> client.get().uri(VIDEOS_PATH).retrieve().toBodilessEntity());

        assertEquals(HttpStatus.FORBIDDEN, actualException.getStatusCode());
    }

    @Test
    void givenATokenWithTheMatchingRole_whenCallTheApi_thenBeAllowed() {
        final var client = clientWith(this.token.bearerOf(RESTRICTED_CLIENT));

        final var actualStatus =
                client.get().uri(CATEGORIES_PATH).retrieve().toBodilessEntity().getStatusCode();

        assertEquals(HttpStatus.OK, actualStatus);
    }

    @Test
    void givenTheAdminToken_whenCallAnyAggregate_thenBeAllowed() {
        final var client = clientWith(this.token.bearer());

        final var actualStatus = client.get().uri(VIDEOS_PATH).retrieve().toBodilessEntity().getStatusCode();

        assertEquals(HttpStatus.OK, actualStatus);
    }

    // Erro em rota LIBERADA tem de chegar com o status dele, e não com o da autorização.
    //
    // Quando não existe handler, o container encaminha para /error, e esse encaminhamento passa
    // pelas regras de acesso de novo: com `anyRequest().hasRole(ADMIN)` e sem liberar o despacho de
    // erro, o /error é recusado e o 404 vira 401. Um caminho embaixo da documentação serve de sonda
    // porque ele é liberado — assim o 401 só pode ter vindo do encaminhamento, e não da requisição.
    //
    // É o mesmo defeito que a subscription-api tinha, corrigido lá liberando ERROR e ASYNC antes de
    // qualquer regra.
    @Test
    void givenAPermittedPathWithNoHandler_whenCalled_thenReturnNotFoundAndNotUnauthorized() {
        final var client = clientWithout();

        final var actualException = assertThrows(
                HttpClientErrorException.class,
                () -> client.get().uri("/swagger-ui/nao-existe.js").retrieve().toBodilessEntity());

        assertEquals(HttpStatus.NOT_FOUND, actualException.getStatusCode());
    }

    @Test
    void givenNoToken_whenCallTheApiDocs_thenBeAllowed() {
        final var client = clientWithout();

        final var actualStatus =
                client.get().uri("/v3/api-docs").retrieve().toBodilessEntity().getStatusCode();

        assertEquals(HttpStatus.OK, actualStatus);
    }

    // A verificação prévia do browser: o primeiro contato de um front de outra origem com esta API.
    //
    // Passa **sem token**, e tem de passar: ela sai antes de qualquer tela ter um. Quem responde é o
    // filtro de CORS, antes da autorização, e é por isso que nenhuma regra de rota precisou liberar
    // OPTIONS.
    //
    // A política mora no CorsConfig, que é a mesma classe nos três serviços — o CopiedSourcesTest
    // prova que continua sendo, e o CorsConfigTest da subscription-api exercita a política inteira. O
    // que este teste prova é a **ligação** dela na cadeia de filtros deste serviço, com Tomcat de
    // verdade: é a linha que se esquece de repetir ao configurar o terceiro serviço.
    @ParameterizedTest
    @ValueSource(strings = {FRONT_ORIGIN, BACK_OFFICE_ORIGIN})
    void givenAFrontOrigin_whenAskPreflightWithoutToken_thenBeAllowed(final String origin) {
        final var client = clientWithout();

        final var actualHeaders = client.method(HttpMethod.OPTIONS)
                .uri(VIDEOS_PATH)
                .header(HttpHeaders.ORIGIN, origin)
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
                .retrieve()
                .toBodilessEntity()
                .getHeaders();

        assertEquals(origin, actualHeaders.getFirst(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    private RestClient clientWithout() {
        return RestClient.create("http://localhost:" + this.port);
    }

    private RestClient clientWith(final String bearer) {
        return RestClient.builder()
                .baseUrl("http://localhost:" + this.port)
                .defaultHeader(HttpHeaders.AUTHORIZATION, bearer)
                .build();
    }
}
