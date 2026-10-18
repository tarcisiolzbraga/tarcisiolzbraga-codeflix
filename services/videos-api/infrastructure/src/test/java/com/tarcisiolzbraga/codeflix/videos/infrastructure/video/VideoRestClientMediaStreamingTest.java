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

// O que prova que o catálogo **repassa** o arquivo, em vez de o montar em memória antes de responder.
//
// Isto precisa de um servidor HTTP de verdade: o `VideoMediaE2ETest` dubla o `VideoClient`, então ele
// nunca exercita o `exchange(fn, false)` — a parte que mantém a resposta do admin aberta até alguém
// escrever o corpo. É um `HttpServer` do próprio JDK, sem dependência nova.
class VideoRestClientMediaStreamingTest {

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

    // **A prova do repasse:** o servidor escreve o primeiro bloco, e só escreve o segundo depois de o
    // primeiro ter chegado ao destino do cliente. Se o catálogo lesse o corpo inteiro antes de
    // devolver o arquivo, o servidor esperaria para sempre — e o teste falha no tempo, com mensagem.
    @Test
    void givenABodyInTwoChunks_whenTheMediaIsRelayed_thenTheSecondOnlyGoesAfterTheFirstArrives()
            throws Exception {
        final var firstArrived = new CountDownLatch(1);
        final var secondWasWritten = new AtomicBoolean(false);
        this.server.createContext("/videos/v1/medias/VIDEO", exchange -> {
            exchange.getResponseHeaders().add(HttpHeaders.CONTENT_TYPE, "video/mp4");
            exchange.getResponseHeaders().add(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"duna.mp4\"");
            exchange.sendResponseHeaders(200, 0);
            try (var body = exchange.getResponseBody()) {
                body.write("primeiro-".getBytes(StandardCharsets.UTF_8));
                body.flush();
                if (!firstArrived.await(5, TimeUnit.SECONDS)) {
                    // Não escreve o segundo: o cliente não entregou o primeiro, então ele bufferizou.
                    return;
                }
                secondWasWritten.set(true);
                body.write("segundo".getBytes(StandardCharsets.UTF_8));
            } catch (final InterruptedException interrupted) {
                Thread.currentThread().interrupt();
            }
        });

        final var relayed = boundToTheStub().mediaOf("v1", "VIDEO", null);

        assertTrue(relayed.isPresent());
        final var destination = new ByteArrayOutputStream();
        relayed.get().content().writeTo(new OutputStream() {
            @Override
            public void write(final int singleByte) throws IOException {
                destination.write(singleByte);
                firstArrived.countDown();
            }

            @Override
            public void write(final byte[] bytes, final int offset, final int length) throws IOException {
                destination.write(bytes, offset, length);
                firstArrived.countDown();
            }
        });

        assertTrue(secondWasWritten.get(), "o segundo bloco nunca foi escrito: o corpo foi bufferizado");
        assertEquals("primeiro-segundo", destination.toString(StandardCharsets.UTF_8));
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

        final var destination = new ByteArrayOutputStream();
        relayed.get().content().writeTo(destination);
        assertEquals("conte", destination.toString(StandardCharsets.UTF_8));
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
