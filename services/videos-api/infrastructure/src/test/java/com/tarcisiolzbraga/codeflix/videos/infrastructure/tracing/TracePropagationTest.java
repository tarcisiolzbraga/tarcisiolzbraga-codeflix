package com.tarcisiolzbraga.codeflix.videos.infrastructure.tracing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.hc.client5.http.async.methods.SimpleRequestBuilder;
import org.apache.hc.client5.http.impl.async.HttpAsyncClients;
import org.apache.hc.core5.http.ContentType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.restclient.RestClientCustomizer;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.client.RestClient;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

// O rastro que entra é o rastro que sai.
//
// Este teste existe porque a falha que ele previne é **silenciosa**: sem o repasse nada quebra, nada
// loga, e os pedidos continuam sendo atendidos — só o rastreamento fragmenta, e isso só aparece
// olhando o Jaeger. Era o estado antes disto, e é o tipo de coisa que volta sem ninguém notar.
//
// Os dois ganchos são exercitados separadamente, porque são mecanismos diferentes: o `RestClient`
// usa um `ClientHttpRequestInterceptor`, e o cliente do Elasticsearch um interceptor do Apache
// HttpClient 5 assíncrono. O segundo é o que conserta o salto medido como quebrado.
class TracePropagationTest {

    private static final String RASTRO = "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01";

    private HttpServer destino;
    private Map<String, String> recebidos;
    private TracePropagationConfig configuracao;

    @BeforeEach
    void startADestinationThatRecordsHeaders() throws IOException {
        this.recebidos = new ConcurrentHashMap<>();
        this.configuracao = new TracePropagationConfig();
        this.destino = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        this.destino.createContext("/", exchange -> {
            exchange.getRequestHeaders()
                    .forEach((nome, valores) -> this.recebidos.put(nome.toLowerCase(), valores.getFirst()));
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
        });
        this.destino.start();
    }

    @AfterEach
    void stopItAndClearTheRequest() {
        this.destino.stop(0);
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void givenAnInboundTrace_whenTheAdminIsCalled_thenItIsCarriedOver() {
        // Given uma requisição em curso com rastro
        givenAnInboundRequestWith("traceparent", RASTRO);

        // When o cliente do admin chama
        clientWithThePropagation().get().uri("/videos/v1").retrieve().toBodilessEntity();

        // Then o rastro chegou do outro lado
        assertEquals(RASTRO, this.recebidos.get("traceparent"));
    }

    // **O salto que estava quebrado.** O cliente do Elasticsearch não passa pelo `RestClient`, então
    // é este gancho — e não o de cima — que liga a busca no índice à requisição que a pediu.
    @Test
    void givenAnInboundTrace_whenTheIndexIsQueried_thenItIsCarriedOver() throws Exception {
        // Given uma requisição em curso com rastro
        givenAnInboundRequestWith("traceparent", RASTRO);

        // When o cliente do índice chama
        whenTheAsyncClientCalls();

        // Then o rastro chegou do outro lado
        assertEquals(RASTRO, this.recebidos.get("traceparent"));
    }

    // Os listeners de Kafka chamam a API do admin **fora** de qualquer requisição HTTP. Sem este
    // caso, um `NullPointerException` ali derrubaria o consumo do CDC — que é pior que rastro
    // fragmentado.
    @Test
    void givenNoInboundRequest_whenTheAdminIsCalled_thenNothingIsAddedAndNothingBreaks() {
        // Given nenhuma requisição em curso
        RequestContextHolder.resetRequestAttributes();

        // When o cliente do admin chama
        clientWithThePropagation().get().uri("/videos/v1").retrieve().toBodilessEntity();

        // Then a chamada aconteceu, e sem rastro inventado
        assertEquals("", this.recebidos.getOrDefault("traceparent", ""));
        assertNull(this.recebidos.get("x-b3-traceid"));
    }

    // Cabeçalho que não veio não é repassado: um rastro parcial inventado aqui seria pior que nenhum,
    // porque o coletor o aceitaria e desenharia uma árvore errada.
    @Test
    void givenOnlyOneOfTheHeaders_whenTheAdminIsCalled_thenOnlyThatOneGoes() {
        // Given uma requisição com só o x-request-id
        givenAnInboundRequestWith("x-request-id", "um-pedido");

        // When o cliente do admin chama
        clientWithThePropagation().get().uri("/videos/v1").retrieve().toBodilessEntity();

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
        final var builder = RestClient.builder().baseUrl(baseUrl());
        // Pelo bean de verdade, e não por uma cópia do interceptor: um teste que reimplementasse a
        // configuração passaria mesmo com o bean quebrado.
        final RestClientCustomizer customizer = this.configuracao.traceOnTheAdminCalls();
        customizer.customize(builder);
        return builder.build();
    }

    private void whenTheAsyncClientCalls() throws Exception {
        final var httpBuilder = HttpAsyncClients.custom();
        this.configuracao.traceOnTheIndexCalls().customize(httpBuilder);
        try (var cliente = httpBuilder.build()) {
            cliente.start();
            final var pedido = SimpleRequestBuilder.post(baseUrl() + "/_search")
                    .setBody("{}", ContentType.APPLICATION_JSON)
                    .build();
            cliente.execute(pedido, null).get();
        }
    }

    private String baseUrl() {
        return "http://127.0.0.1:%d".formatted(this.destino.getAddress().getPort());
    }
}
