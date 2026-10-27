package com.tarcisiolzbraga.codeflix.subscription.infrastructure.tracing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.client.RestClient;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

// O rastro que entra é o rastro que sai, nas chamadas ao Keycloak.
//
// Este teste existe porque a falha que ele previne é **silenciosa**: sem o repasse nada quebra, nada
// loga, e o cadastro continua funcionando — só o rastreamento fragmenta, e isso só aparece olhando o
// Jaeger.
class TracePropagationTest {

    private static final String RASTRO = "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01";

    private HttpServer keycloakDublado;
    private Map<String, String> recebidos;
    private TracePropagationConfig configuracao;

    @BeforeEach
    void startADestinationThatRecordsHeaders() throws IOException {
        this.recebidos = new ConcurrentHashMap<>();
        this.configuracao = new TracePropagationConfig();
        this.keycloakDublado = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        this.keycloakDublado.createContext("/", exchange -> {
            exchange.getRequestHeaders()
                    .forEach((nome, valores) -> this.recebidos.put(nome.toLowerCase(), valores.getFirst()));
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
        });
        this.keycloakDublado.start();
    }

    @AfterEach
    void stopItAndClearTheRequest() {
        this.keycloakDublado.stop(0);
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void givenAnInboundTrace_whenKeycloakIsCalled_thenItIsCarriedOver() {
        // Given uma requisição em curso com rastro
        givenAnInboundRequestWith("traceparent", RASTRO);

        // When o cliente do Keycloak chama
        clientWithThePropagation().get().uri("/admin/realms/codeflix/users").retrieve().toBodilessEntity();

        // Then o rastro chegou do outro lado
        assertEquals(RASTRO, this.recebidos.get("traceparent"));
    }

    // O job da outbox fala com o Keycloak **fora** de qualquer requisição HTTP. Sem este caso, um erro
    // ali derrubaria a entrega de eventos — que é pior que rastro fragmentado.
    @Test
    void givenNoInboundRequest_whenKeycloakIsCalled_thenNothingIsAddedAndNothingBreaks() {
        // Given nenhuma requisição em curso
        RequestContextHolder.resetRequestAttributes();

        // When o cliente do Keycloak chama
        clientWithThePropagation().get().uri("/admin/realms/codeflix/users").retrieve().toBodilessEntity();

        // Then a chamada aconteceu, e sem rastro inventado
        assertNull(this.recebidos.get("traceparent"));
        assertNull(this.recebidos.get("x-b3-traceid"));
    }

    // Cabeçalho que não veio não é repassado: um rastro parcial inventado aqui seria pior que nenhum,
    // porque o coletor o aceitaria e desenharia uma árvore errada.
    @Test
    void givenOnlyOneOfTheHeaders_whenKeycloakIsCalled_thenOnlyThatOneGoes() {
        // Given uma requisição com só o x-request-id
        givenAnInboundRequestWith("x-request-id", "um-pedido");

        // When o cliente do Keycloak chama
        clientWithThePropagation().get().uri("/admin/realms/codeflix/users").retrieve().toBodilessEntity();

        // Then só ele foi
        assertEquals("um-pedido", this.recebidos.get("x-request-id"));
        assertNull(this.recebidos.get("traceparent"));
    }

    private void givenAnInboundRequestWith(final String nome, final String valor) {
        final var entrando = new MockHttpServletRequest();
        entrando.addHeader(nome, valor);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(entrando));
    }

    private RestClient clientWithThePropagation() {
        final var builder = RestClient.builder()
                .baseUrl("http://127.0.0.1:%d".formatted(this.keycloakDublado.getAddress().getPort()));
        // Pelo bean de verdade, e não por uma cópia do interceptor: um teste que reimplementasse a
        // configuração passaria mesmo com o bean quebrado.
        this.configuracao.traceOnTheKeycloakCalls().customize(builder);
        return builder.build();
    }
}
