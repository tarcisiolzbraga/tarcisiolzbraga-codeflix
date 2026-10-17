package com.tarcisiolzbraga.codeflix.videos.e2e.video;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tarcisiolzbraga.codeflix.videos.application.video.VideoOutput;
import com.tarcisiolzbraga.codeflix.videos.application.video.get.GetVideoUseCase;
import com.tarcisiolzbraga.codeflix.videos.domain.video.VideoFlags;
import com.tarcisiolzbraga.codeflix.videos.infrastructure.E2ETest;
import com.tarcisiolzbraga.codeflix.videos.infrastructure.KeycloakTestToken;
import com.tarcisiolzbraga.codeflix.videos.infrastructure.video.VideoClient;
import com.tarcisiolzbraga.codeflix.videos.infrastructure.video.models.VideoMediaFile;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.RestClient;

// A rota que serve mídia a quem assina, de ponta a ponta: token emitido por um Keycloak de verdade,
// atravessando o Tomcat e o filtro, até o repasse.
//
// O cliente do admin é dublado de propósito — o que esta classe prova é a **autorização** e o
// **repasse**, não a conversa HTTP com o admin, que é o contrato dele e não muda aqui.
@E2ETest
class VideoMediaE2ETest {

    private static final String A_VIDEO = "00000024-0000-0000-0000-000000000000";
    private static final byte[] CONTENT = "nao-e-um-mp4-de-verdade".getBytes(StandardCharsets.UTF_8);

    @Value("${local.server.port}")
    private int port;

    @Autowired
    private KeycloakTestToken token;

    @MockitoBean
    private VideoClient videoClient;

    // Dublado para o teste poder dizer se o título é aberto: é o que a regra do `opened` consulta, e
    // controlá-lo aqui mantém o caso explícito em vez de depender do que está no índice.
    @MockitoBean
    private GetVideoUseCase getVideoUseCase;

    // **O dublê precisa ser armado com Optional.empty() à mão.** `GetVideoUseCase` herda
    // `UseCase<IN, OUT>`, então o apagamento de tipo faz `execute` devolver `Object` no bytecode — e
    // o Mockito, sem saber que a resposta é um Optional, preenche `null`. Sem este @BeforeEach, o
    // caminho do título não aberto estourava NullPointerException e virava 500 em vez de 403.
    @BeforeEach
    void noOpenTitleUnlessTheCaseSaysSo() {
        when(this.getVideoUseCase.execute(any())).thenReturn(Optional.empty());
    }

    @Test
    void givenTheSubscriberRole_whenAsksForTheMedia_thenServeItWithTheOriginalContentType() {
        // Given o admin tendo o arquivo
        when(this.videoClient.mediaOf(any(), any(), any()))
                .thenReturn(Optional.of(new VideoMediaFile("video/mp4", "filme.mp4", CONTENT, null)));

        // When quem assina pede a mídia
        final var actual = clientWith(this.token.catalogTest())
                .get()
                .uri("/videos/{id}/medias/{type}", A_VIDEO, "VIDEO")
                .retrieve()
                .toEntity(byte[].class);

        // Then o arquivo chega com o tipo do admin, e não genérico
        assertEquals(HttpStatus.OK, actual.getStatusCode());
        assertEquals("video/mp4", String.valueOf(actual.getHeaders().getContentType()));
        assertArrayEquals(CONTENT, actual.getBody());
    }

    // inline, e não attachment: o player aponta para esta rota, e attachment faria o browser baixar
    // em vez de reproduzir.
    @Test
    void givenTheSubscriberRole_whenAsksForTheMedia_thenTheDispositionIsInlineWithTheOriginalName() {
        // Given o admin tendo o arquivo
        when(this.videoClient.mediaOf(any(), any(), any()))
                .thenReturn(Optional.of(new VideoMediaFile("video/mp4", "filme.mp4", CONTENT, null)));

        // When pede a mídia
        final var actual = clientWith(this.token.catalogTest())
                .get()
                .uri("/videos/{id}/medias/{type}", A_VIDEO, "VIDEO")
                .retrieve()
                .toBodilessEntity();

        // Then o nome original vem junto, para quem salvar não receber o nome da rota
        final var disposition = String.valueOf(actual.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION));
        assertTrue(disposition.startsWith("inline"), disposition);
        assertTrue(disposition.contains("filme.mp4"), disposition);
    }

    // Título cadastrado sem upload existe no acervo e não tem o que reproduzir: 404 limpo, e não erro.
    @Test
    void givenAVideoWithoutThatMedia_whenAsksForIt_thenReturnNotFound() {
        // Given o admin sem aquele tipo de mídia
        when(this.videoClient.mediaOf(any(), any(), any())).thenReturn(Optional.empty());

        // When pede a mídia / Then é 404
        assertEquals(HttpStatus.NOT_FOUND, statusOf(this.token.catalogTest()));
    }

    @Test
    void givenNoToken_whenAsksForTheMedia_thenRefuseItAsUnauthorized() {
        // Given ninguém identificado
        // When pede a mídia / Then 401 — e o repasse não acontece
        assertEquals(HttpStatus.UNAUTHORIZED, statusOf(null));
    }

    // O client sem papel algum: identificou-se, e ainda assim não pode assistir. É o que separa "não
    // sei quem é" de "sei quem é, e não basta".
    @Test
    void givenATokenWithoutTheRole_whenAsksForTheMedia_thenRefuseItAsForbidden() {
        // Given um token sem o papel do catálogo
        // When pede a mídia / Then 403
        assertEquals(HttpStatus.FORBIDDEN, statusOf(this.token.bearerOf(KeycloakTestToken.STRANGER)));
    }

    // O administrador alcança a mídia como alcança o resto do catálogo.
    @Test
    void givenTheAdminRole_whenAsksForTheMedia_thenServeIt() {
        // Given o admin tendo o arquivo
        when(this.videoClient.mediaOf(any(), any(), any()))
                .thenReturn(Optional.of(new VideoMediaFile("video/mp4", "filme.mp4", CONTENT, null)));

        // When o administrador pede / Then é servido
        assertEquals(HttpStatus.OK, statusOf(this.token.admin()));
    }

    // A rota que emite o endereço assinado, e o motivo dela: `<video src>` não carrega cabeçalho de
    // Authorization, então a URL precisa se autorizar sozinha.
    @Test
    void givenTheSubscriberRole_whenAsksForALink_thenGetASignedPathAndWhenItExpires() {
        // Given quem assina
        // When pede o link
        final var actual = clientWith(this.token.catalogTest())
                .get()
                .uri("/videos/{id}/medias/{type}/link", A_VIDEO, "VIDEO")
                .retrieve()
                .body(LinkView.class);

        // Then vem um caminho relativo assinado e a validade
        assertTrue(actual.path().startsWith("/videos/" + A_VIDEO + "/medias/VIDEO?"), actual.path());
        assertTrue(actual.path().contains("signature="), actual.path());
        assertTrue(actual.expiresAt().isAfter(Instant.now()), String.valueOf(actual.expiresAt()));
    }

    // **O caso que prova o desenho inteiro**: com a assinatura, a mídia é servida sem token nenhum —
    // que é a única forma de o `<video>` do browser reproduzir sem baixar o arquivo antes.
    @Test
    void givenASignedPath_whenAsksForTheMediaWithoutAnyToken_thenServeIt() {
        // Given o admin tendo o arquivo, e um link assinado emitido a quem assina
        when(this.videoClient.mediaOf(any(), any(), any()))
                .thenReturn(Optional.of(new VideoMediaFile("video/mp4", "filme.mp4", CONTENT, null)));
        final var link = clientWith(this.token.catalogTest())
                .get()
                .uri("/videos/{id}/medias/{type}/link", A_VIDEO, "VIDEO")
                .retrieve()
                .body(LinkView.class);

        // When o endereço assinado é buscado **sem** Authorization
        final var actual = clientWith(null).get().uri(link.path()).retrieve().toEntity(byte[].class);

        // Then o arquivo é servido
        assertEquals(HttpStatus.OK, actual.getStatusCode());
        assertArrayEquals(CONTENT, actual.getBody());
    }

    @Test
    void givenATamperedSignature_whenAsksForTheMedia_thenRefuseItAsForbidden() {
        // Given um link assinado, com o selo mexido
        final var link = clientWith(this.token.catalogTest())
                .get()
                .uri("/videos/{id}/medias/{type}/link", A_VIDEO, "VIDEO")
                .retrieve()
                .body(LinkView.class);
        final var mexido = link.path().replaceAll("signature=.*$", "signature=nao-e-o-selo");

        // When é buscado / Then 403: quem apresentou tinha direito quando pediu — o selo é que não
        // confere
        final var actual = clientWith(null)
                .get()
                .uri(mexido)
                .exchange((request, response) -> response.getStatusCode());
        assertEquals(HttpStatus.FORBIDDEN, actual);
    }

    @Test
    void givenNoToken_whenAsksForALink_thenRefuseItAsUnauthorized() {
        // Given ninguém identificado
        // When pede o link / Then 401
        final var actual = clientWith(null)
                .get()
                .uri("/videos/{id}/medias/{type}/link", A_VIDEO, "VIDEO")
                .exchange((request, response) -> response.getStatusCode());
        assertEquals(HttpStatus.UNAUTHORIZED, actual);
    }

    // A regra nova do `opened`: título aberto dispensa o papel de assinante, e **não** dispensa estar
    // identificado — sem isso a rota viraria superfície de banda sem limite de taxa.
    @Test
    void givenAnOpenTitleAndATokenWithoutTheRole_whenAsksForTheMedia_thenServeIt() {
        // Given um título marcado como aberto no catálogo
        when(this.getVideoUseCase.execute(any())).thenReturn(Optional.of(openTitle()));
        when(this.videoClient.mediaOf(any(), any(), any()))
                .thenReturn(Optional.of(new VideoMediaFile("video/mp4", "filme.mp4", CONTENT, null)));

        // When alguém identificado e sem o papel pede / Then é servido
        assertEquals(HttpStatus.OK, statusOf(this.token.bearerOf(KeycloakTestToken.STRANGER)));
    }

    @Test
    void givenAnOpenTitleAndNoToken_whenAsksForTheMedia_thenStillRefuseIt() {
        // Given um título aberto
        when(this.getVideoUseCase.execute(any())).thenReturn(Optional.of(openTitle()));

        // When ninguém identificado pede / Then 401: "aberto" vale para quem entrou, não para o mundo
        assertEquals(HttpStatus.UNAUTHORIZED, statusOf(null));
    }

    private static VideoOutput openTitle() {
        return new VideoOutput(
                A_VIDEO, null, new VideoFlags(true, true, true), null, null, Instant.now(), Instant.now());
    }

    record LinkView(String path, Instant expiresAt) {}

    // O repasse do `Range`: é o que permite ao player buscar na linha de tempo, e o que fez esta rota
    // deixar de obrigar o browser a baixar o arquivo inteiro.
    @Test
    void givenARangeHeader_whenAsksForTheMedia_thenRelayTheRangeAndAnswerPartialContent() {
        // Given o admin devolvendo um trecho, com o Content-Range que o armazenamento calculou
        when(this.videoClient.mediaOf(any(), any(), any()))
                .thenReturn(Optional.of(new VideoMediaFile("video/mp4", "filme.mp4", CONTENT, "bytes 0-9/100")));

        // When o player pede um trecho
        final var actual = clientWith(this.token.catalogTest())
                .get()
                .uri("/videos/{id}/medias/{type}", A_VIDEO, "VIDEO")
                .header(HttpHeaders.RANGE, "bytes=0-9")
                .retrieve()
                .toEntity(byte[].class);

        // Then a resposta é 206 com o intervalo, e o cabeçalho foi repassado **como veio**
        assertEquals(HttpStatus.PARTIAL_CONTENT, actual.getStatusCode());
        assertEquals("bytes 0-9/100", actual.getHeaders().getFirst(HttpHeaders.CONTENT_RANGE));
        verify(this.videoClient).mediaOf(A_VIDEO, "VIDEO", "bytes=0-9");
    }

    // O status sai do que o admin devolveu, e não da presença do cabeçalho de pedido: o admin pode
    // ignorar um Range que não souber atender, e aí 206 com o arquivo inteiro enganaria o player.
    @Test
    void givenARangeThatTheAdminIgnored_whenAsksForTheMedia_thenAnswerTwoHundredAndNotPartial() {
        // Given o admin devolvendo o arquivo inteiro, sem Content-Range, apesar do pedido
        when(this.videoClient.mediaOf(any(), any(), any()))
                .thenReturn(Optional.of(new VideoMediaFile("video/mp4", "filme.mp4", CONTENT, null)));

        // When o player pede um trecho
        final var actual = clientWith(this.token.catalogTest())
                .get()
                .uri("/videos/{id}/medias/{type}", A_VIDEO, "VIDEO")
                .header(HttpHeaders.RANGE, "bytes=0-9")
                .exchange((request, response) -> response.getStatusCode());

        // Then é 200: a resposta descreve o que foi servido, não o que foi pedido
        assertEquals(HttpStatus.OK, actual);
    }

    // `Accept-Ranges` é o que faz o player saber que pode pedir trecho.
    @Test
    void givenTheMediaServed_whenReadTheHeaders_thenRangesAreAnnounced() {
        // Given o admin devolvendo o arquivo
        when(this.videoClient.mediaOf(any(), any(), any()))
                .thenReturn(Optional.of(new VideoMediaFile("video/mp4", "filme.mp4", CONTENT, null)));

        // When pede a mídia
        final var actual = clientWith(this.token.catalogTest())
                .get()
                .uri("/videos/{id}/medias/{type}", A_VIDEO, "VIDEO")
                .retrieve()
                .toBodilessEntity();

        // Then a rota anuncia que aceita trecho
        assertEquals("bytes", actual.getHeaders().getFirst(HttpHeaders.ACCEPT_RANGES));
    }

    private HttpStatusCode statusOf(final String bearer) {
        return clientWith(bearer)
                .get()
                .uri("/videos/{id}/medias/{type}", A_VIDEO, "VIDEO")
                .exchange((request, response) -> response.getStatusCode());
    }

    private RestClient clientWith(final String bearer) {
        final var builder = RestClient.builder().baseUrl("http://localhost:%d/api".formatted(this.port));
        return bearer == null ? builder.build() : builder.defaultHeader(HttpHeaders.AUTHORIZATION, bearer).build();
    }
}
