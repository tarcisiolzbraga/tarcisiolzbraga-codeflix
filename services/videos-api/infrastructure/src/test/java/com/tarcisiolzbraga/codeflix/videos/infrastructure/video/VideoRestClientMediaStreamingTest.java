package com.tarcisiolzbraga.codeflix.videos.infrastructure.video;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sun.net.httpserver.HttpServer;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.util.Timeout;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
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

    // O mesmo cliente, com **uma** conexão de pool e espera curta por ela. É o que torna um
    // vazamento observável: se a conexão de uma chamada não for devolvida, a chamada seguinte não
    // arranca vaga e estoura no tempo de espera, em vez de passar e esconder o vazamento.
    //
    // O servidor ganha um executor próprio aqui: no dubro padrão os handlers correm na thread de
    // despacho, e um handler preso escrevendo para um cliente que desistiu travaria a chamada
    // seguinte — o teste acusaria vazamento onde não há.
    private VideoRestClient boundToTheStubWithASingleConnection() {
        this.server.setExecutor(Executors.newCachedThreadPool());
        this.server.start();
        final var pool = PoolingHttpClientConnectionManagerBuilder.create()
                .setMaxConnTotal(1)
                .setMaxConnPerRoute(1)
                .build();
        final var httpClient = HttpClients.custom()
                .setConnectionManager(pool)
                .setDefaultRequestConfig(RequestConfig.custom()
                        .setConnectionRequestTimeout(Timeout.ofSeconds(2))
                        .build())
                .build();
        final var base = "http://127.0.0.1:%d".formatted(this.server.getAddress().getPort());
        return new VideoRestClient(RestClient.builder()
                .baseUrl(base)
                .requestFactory(new HttpComponentsClientHttpRequestFactory(httpClient))
                .build());
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

    // **Cliente que desiste do vídeo no meio não deixa conexão para trás.** É o caso comum de quem
    // fecha a aba, e o que o `try-with-resources` do repasse existe para cobrir: sem ele a conexão
    // com o admin ficaria presa no `transferTo` que falhou, uma por desistência.
    //
    // O corpo é grande de propósito: a escrita falha com a maior parte dele ainda por ler, que é o
    // caso difícil — a conexão não dá para reaproveitar, e tem de ser descartada **e** a vaga
    // devolvida. Devolver a vaga é o que este teste mede.
    @Test
    void givenTheClientGivingUpMidWrite_whenTheMediaIsRelayed_thenTheConnectionIsReleased() throws Exception {
        serveTheWholeFile(LARGE_FILE);
        final var client = boundToTheStubWithASingleConnection();

        final var abandoned = client.mediaOf("v1", "VIDEO", null);
        assertTrue(abandoned.isPresent());
        assertThrows(IOException.class, () -> abandoned.get().content().writeTo(aDestinationThatFails()));

        final var destination = new ByteArrayOutputStream();
        client.mediaOf("v1", "VIDEO", null)
                .orElseThrow(() -> new AssertionError("a segunda chamada não recebeu arquivo"))
                .content()
                .writeTo(destination);
        assertEquals(LARGE_FILE.length(), destination.size());
    }

    // O mesmo para o 404: ele fecha a resposta pelo caminho de cima, antes de haver corpo para
    // repassar. O comentário do cliente afirmava isso desde o começo; aqui a vaga do pool prova.
    @Test
    void givenTheAdminAnswering404_whenTheMediaIsRelayed_thenTheConnectionIsReleased() throws Exception {
        this.server.createContext("/videos/v1/medias/VIDEO", exchange -> {
            exchange.sendResponseHeaders(404, -1);
            exchange.close();
        });
        final var client = boundToTheStubWithASingleConnection();

        assertTrue(client.mediaOf("v1", "VIDEO", null).isEmpty());

        assertTrue(
                client.mediaOf("v1", "VIDEO", null).isEmpty(),
                "a segunda chamada não conseguiu conexão: o 404 vazou a primeira");
    }

    private static final String LARGE_FILE = "conteudo".repeat(64 * 1024);

    private void serveTheWholeFile(final String content) {
        this.server.createContext("/videos/v1/medias/VIDEO", exchange -> {
            exchange.getResponseHeaders().add(HttpHeaders.CONTENT_TYPE, "video/mp4");
            final var body = content.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            // O cliente que desiste quebra esta escrita; é o comportamento sob teste, e não falha do
            // dubro. Sem o `catch` o `HttpServer` registra a pilha e polui a saída.
            try (var out = exchange.getResponseBody()) {
                out.write(body);
            } catch (final IOException expectedWhenTheClientGivesUp) {
                exchange.close();
            }
        });
    }

    private static OutputStream aDestinationThatFails() {
        return new OutputStream() {
            @Override
            public void write(final int singleByte) throws IOException {
                throw new IOException("o cliente desistiu do vídeo");
            }

            @Override
            public void write(final byte[] bytes, final int offset, final int length) throws IOException {
                throw new IOException("o cliente desistiu do vídeo");
            }
        };
    }
}
