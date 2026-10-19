package com.tarcisiolzbraga.codeflix.videos.infrastructure.video;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sun.net.httpserver.HttpServer;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;

// O que o repasse de mídia promete, contra um servidor HTTP de verdade — um `HttpServer` do próprio
// JDK, sem dependência nova. O `VideoMediaE2ETest` dubla o `VideoClient`, então ele nunca exercita
// esta parte.
//
// A versão anterior destes testes afirmava que o repasse era **em fluxo**, com o servidor só
// escrevendo o segundo bloco depois de o primeiro chegar ao cliente. Aquilo funcionava, e foi
// desfeito: o porquê está no `VideoMediaFile`. O que ficou é o que ainda se promete — os metadados
// lidos dos cabeçalhos, o tamanho repassado, e o corpo entregue.
class VideoRestClientMediaTest {

    private HttpServer server;
    private VideoRestClient client;

    @BeforeEach
    void startAStubbedAdmin() throws IOException {
        this.server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    }

    @AfterEach
    void stopIt() {
        this.server.stop(0);
    }

    private VideoRestClient boundToTheStub() {
        this.server.start();
        final var base = "http://127.0.0.1:%d".formatted(this.server.getAddress().getPort());
        return new VideoRestClient(RestClient.builder().baseUrl(base).build());
    }

    // Os cabeçalhos são lidos **antes** do corpo: é com eles que a rota decide 200 ou 206, e é por
    // isso que o corpo pode ficar para depois.
    @Test
    void givenAPartialResponse_whenTheMediaIsRelayed_thenTheMetadataComesBeforeTheBody() throws Exception {
        this.server.createContext("/videos/v1/medias/VIDEO", exchange -> {
            exchange.getResponseHeaders().add(HttpHeaders.CONTENT_TYPE, "video/mp4");
            exchange.getResponseHeaders().add(HttpHeaders.CONTENT_RANGE, "bytes 0-4/19");
            exchange.getResponseHeaders().add(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"duna.mp4\"");
            exchange.sendResponseHeaders(206, 0);
            try (var body = exchange.getResponseBody()) {
                body.write("conte".getBytes(StandardCharsets.UTF_8));
            }
        });

        final var relayed = boundToTheStub().mediaOf("v1", "VIDEO", "bytes=0-4");

        assertTrue(relayed.isPresent());
        assertEquals("video/mp4", relayed.get().contentType());
        assertEquals("bytes 0-4/19", relayed.get().contentRange());
        assertEquals("duna.mp4", relayed.get().filename());

        assertEquals("conte", new String(relayed.get().content(), StandardCharsets.UTF_8));
    }

    // O tamanho que o admin informou é repassado: é com ele que a rota do catálogo põe
    // `Content-Length` na própria resposta, em vez de sair em chunks.
    @Test
    void givenTheAdminDeclaringTheLength_whenTheMediaIsRelayed_thenItIsCarriedOver() throws Exception {
        this.server.createContext("/videos/v1/medias/VIDEO", exchange -> {
            exchange.getResponseHeaders().add(HttpHeaders.CONTENT_TYPE, "video/mp4");
            final var body = "conteudo".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            try (var out = exchange.getResponseBody()) {
                out.write(body);
            }
        });

        final var relayed = boundToTheStub().mediaOf("v1", "VIDEO", null);

        assertTrue(relayed.isPresent());
        assertEquals("conteudo".length(), relayed.get().length());
    }

    // 404 fecha a resposta e devolve vazio: vídeo sem aquela mídia é caso comum, e deixar a conexão
    // pendurada por ele vazaria uma por pedido.
    @Test
    void givenTheAdminAnswering404_whenTheMediaIsRelayed_thenReceiveEmpty() {
        this.server.createContext("/videos/v1/medias/VIDEO", exchange -> {
            exchange.sendResponseHeaders(404, -1);
            exchange.close();
        });

        final Optional<?> relayed = boundToTheStub().mediaOf("v1", "VIDEO", null);

        assertTrue(relayed.isEmpty());
    }

    @Test
    void givenNoContentDisposition_whenTheMediaIsRelayed_thenTheFilenameFallsBack() {
        this.server.createContext("/videos/v1/medias/VIDEO", exchange -> {
            exchange.getResponseHeaders().add(HttpHeaders.CONTENT_TYPE, "video/mp4");
            exchange.sendResponseHeaders(200, 0);
            try (var body = exchange.getResponseBody()) {
                body.write("x".getBytes(StandardCharsets.UTF_8));
            }
        });

        final var relayed = boundToTheStub().mediaOf("v1", "VIDEO", null);

        assertTrue(relayed.isPresent());
        assertFalse(relayed.get().filename().isBlank());
    }
}
