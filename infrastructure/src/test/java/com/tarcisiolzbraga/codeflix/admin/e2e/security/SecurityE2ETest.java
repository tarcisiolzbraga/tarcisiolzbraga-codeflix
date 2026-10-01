package com.tarcisiolzbraga.codeflix.admin.e2e.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.tarcisiolzbraga.codeflix.admin.infrastructure.E2ETest;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.KeycloakTestToken;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
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

    @Test
    void givenNoToken_whenCallTheApiDocs_thenBeAllowed() {
        final var client = clientWithout();

        final var actualStatus =
                client.get().uri("/v3/api-docs").retrieve().toBodilessEntity().getStatusCode();

        assertEquals(HttpStatus.OK, actualStatus);
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
