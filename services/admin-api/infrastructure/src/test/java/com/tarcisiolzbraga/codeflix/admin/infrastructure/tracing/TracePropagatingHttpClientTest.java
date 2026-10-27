package com.tarcisiolzbraga.codeflix.admin.infrastructure.tracing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.http.apache5.Apache5HttpClient;
import software.amazon.awssdk.services.s3.S3Client;

// O rastro da requisição que pediu a mídia chega ao armazenamento.
//
// Contra um `HttpServer` do JDK no lugar do Garage: o que está sob teste é o gancho do SDK, não o
// armazenamento. Ele registra os cabeçalhos que recebeu e responde qualquer coisa — o SDK vai
// reclamar da resposta, e isso não importa, porque a requisição **já saiu** quando ele reclama.
class TracePropagatingHttpClientTest {

    private static final String RASTRO = "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01";

    private HttpServer garageDublado;
    private Map<String, String> recebidos;

    @BeforeEach
    void startAStubbedStorage() throws IOException {
        this.recebidos = new ConcurrentHashMap<>();
        this.garageDublado = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        this.garageDublado.createContext("/", exchange -> {
            exchange.getRequestHeaders()
                    .forEach((nome, valores) -> this.recebidos.put(nome.toLowerCase(), valores.getFirst()));
            final var corpo = "nao-importa".getBytes();
            exchange.sendResponseHeaders(200, corpo.length);
            try (var saida = exchange.getResponseBody()) {
                saida.write(corpo);
            }
        });
        this.garageDublado.start();
    }

    @AfterEach
    void stopItAndClearTheRequest() {
        this.garageDublado.stop(0);
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void givenAnInboundTrace_whenTheStorageIsCalled_thenItIsCarriedOver() {
        // Given uma requisição em curso com rastro
        givenAnInboundRequestWith("traceparent", RASTRO);

        // When o cliente do armazenamento busca um arquivo
        whenTheStorageIsCalled();

        // Then o rastro chegou do outro lado
        assertEquals(RASTRO, this.recebidos.get("traceparent"));
    }

    // **O rastro NÃO entra na assinatura, e este é o teste que importa.**
    //
    // A primeira versão disto punha o cabeçalho antes de o SDK assinar, por um
    // `ExecutionInterceptor`. O `traceparent` entrava em `SignedHeaders`, e o Garage recusava com
    // `403 Invalid signature` em **todo** pedido de mídia — porque o sidecar do Istio reescreve
    // `traceparent` em trânsito, criando um span novo para o salto.
    //
    // O teste que havia antes afirmava que a requisição continuava assinada. Continuava, e isso não
    // provava nada: dublê não confere assinatura. A afirmação era verdadeira e inútil, e quem a
    // desmentiu foi o cluster.
    @Test
    void givenAnInboundTrace_whenTheStorageIsCalled_thenTheTraceIsNotSigned() {
        // Given uma requisição em curso com rastro
        givenAnInboundRequestWith("traceparent", RASTRO);

        // When o cliente do armazenamento busca um arquivo
        whenTheStorageIsCalled();

        // Then a requisição está assinada, e o rastro ficou fora da assinatura
        final var autorizacao = this.recebidos.getOrDefault("authorization", "");
        assertTrue(autorizacao.contains("Signature="), "a requisição saiu sem assinatura: " + autorizacao);
        final var assinados = autorizacao.replaceFirst(".*SignedHeaders=([^,]+).*", "$1");
        assertFalse(
                assinados.contains("traceparent"),
                "o rastro entrou na assinatura, e a malha vai reescrevê-lo: " + assinados);
    }

    // O relay da outbox toca o armazenamento **fora** de qualquer requisição HTTP. Sem este caso, um
    // erro ali derrubaria a entrega de eventos — que é pior que rastro fragmentado.
    @Test
    void givenNoInboundRequest_whenTheStorageIsCalled_thenNothingIsAddedAndNothingBreaks() {
        // Given nenhuma requisição em curso
        RequestContextHolder.resetRequestAttributes();

        // When o cliente do armazenamento busca um arquivo
        whenTheStorageIsCalled();

        // Then a chamada aconteceu, e sem rastro inventado
        assertNull(this.recebidos.get("traceparent"));
        assertTrue(this.recebidos.containsKey("authorization"), "a requisição nem saiu");
    }

    private void givenAnInboundRequestWith(final String nome, final String valor) {
        final var entrando = new MockHttpServletRequest();
        entrando.addHeader(nome, valor);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(entrando));
    }

    private void whenTheStorageIsCalled() {
        try (var cliente = stubbedClient()) {
            cliente.getObject(pedido -> pedido.bucket("acervo").key("duna.mp4"));
        } catch (final RuntimeException oDubleNaoFalaS3) {
            // Esperado: o dublê não devolve XML de S3. A requisição já saiu, que é o que se mede.
        }
    }

    private S3Client stubbedClient() {
        return S3Client.builder()
                .endpointOverride(URI.create(
                        "http://127.0.0.1:%d".formatted(this.garageDublado.getAddress().getPort())))
                .region(Region.of("us-east-1"))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create("uma-chave", "um-segredo")))
                .forcePathStyle(true)
                .requestChecksumCalculation(RequestChecksumCalculation.WHEN_REQUIRED)
                // O mesmo gancho do bean de verdade: um teste que reimplementasse o interceptor
                // passaria mesmo com a configuração quebrada.
                .httpClientBuilder(TracePropagatingHttpClient.wrapping(Apache5HttpClient.builder()))
                .build();
    }
}
