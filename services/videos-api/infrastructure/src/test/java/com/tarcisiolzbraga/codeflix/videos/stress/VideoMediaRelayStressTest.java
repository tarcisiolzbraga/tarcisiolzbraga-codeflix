package com.tarcisiolzbraga.codeflix.videos.stress;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.sun.net.httpserver.HttpServer;
import com.tarcisiolzbraga.codeflix.videos.infrastructure.KeycloakTestToken;
import com.tarcisiolzbraga.codeflix.videos.infrastructure.video.api.MediaLinks;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestClient;

// Mede a taxa de falha do **repasse** do catálogo sob muitas respostas curtas em sequência.
//
// **Por que isto existe.** O repasse em fluxo foi desfeito em 19/10 por uma instabilidade que não se
// conseguiu fechar na época; o conserto saiu em 26/10, na admin-api, e o instrumento que o provou
// vive lá — preso ao próprio admin, porque sobe o app dele. Devolver o fluxo ao catálogo sem um
// instrumento **daqui** repetiria o erro de 19/10 ao contrário: declarar conserto porque o CI ficou
// verde, que é exatamente o que o README da admin-api registra não valer nada.
//
// **O que é medido, e o que não é.** O dublê do admin é um `HttpServer` do JDK, e não um container:
// o que está sob teste é o caminho `exchange(fn, false)` -> `StreamingResponseBody` -> despacho
// assíncrono do Tomcat, com pool compartilhado. A conversa HTTP com o admin de verdade é contrato
// dele, tem teste próprio, e trazê-la para cá mediria a rede do CI junto.
//
// **A porta usada é a do selo, de propósito.** A rota tem duas, e a assinada não passa por token
// nem pelo índice: ela vai direto ao repasse. Menos coisa no meio entre a carga e o que se quer
// medir — e nenhuma dependência de dado semeado.
//
// A asserção é de **zero falha**, e não de uma taxa aceitável: resposta corrompida não tem cota. Se
// falhar, a mensagem traz a taxa e a quebra por exceção.
@StressTest
class VideoMediaRelayStressTest {

    // Pequeno de propósito, como no irmão da admin-api: a condição documentada é de respostas
    // assíncronas **curtas** em sequência, e arquivo grande daria outro regime — menos trocas de
    // resposta por segundo, menos chance de a corrida aparecer.
    private static final byte[] CONTEUDO = "imagem pequena, de propósito".getBytes(StandardCharsets.UTF_8);
    private static final String UM_VIDEO = "00000024-0000-0000-0000-000000000000";
    private static final int THREADS = Integer.getInteger("stress.threads", 8);
    private static final int POR_THREAD = Integer.getInteger("stress.iteracoes", 150);
    private static final long LIMITE_SEGUNDOS = 300;

    // Estático e iniciado no carregamento da classe: o `@DynamicPropertySource` roda antes do
    // contexto subir, e precisa da porta já escolhida para apontar o catálogo para cá.
    private static final HttpServer ADMIN_DUBLADO = umAdminQueServeORepasse();

    @Value("${local.server.port}")
    private int port;

    @Autowired
    private MediaLinks links;

    private RestClient comPool;
    private final Map<String, String> exemplos = new ConcurrentHashMap<>();

    // **Três propriedades, e cada uma por um motivo.**
    //
    // A primeira aponta o catálogo para o dublê, que é o ponto do teste.
    //
    // As outras duas existem porque este é o **primeiro** teste a exercitar o cliente de verdade do
    // admin: todos os E2E dublam o `VideoClient`, então o caminho do token de client_credentials
    // nunca rodou em teste. Sem elas o perfil `development` manda o catálogo pedir token em
    // `localhost:8081`, que no CI não existe — e a rota devolve 500 em 100% dos pedidos, medindo
    // conexão recusada em vez de repasse.
    //
    // O endereço do token é derivado do emissor que o container registrou, e não escrito à mão: o
    // Keycloak sobe em porta sorteada, e repetir a porta aqui seria fixar o que o container decide.
    @DynamicPropertySource
    static void pointTheCatalogAtTheStub(final DynamicPropertyRegistry registry) {
        registry.add(
                "admin-api.base-url",
                () -> "http://127.0.0.1:%d".formatted(ADMIN_DUBLADO.getAddress().getPort()));
        registry.add(
                "spring.security.oauth2.client.provider.keycloak.token-uri",
                () -> "${keycloak.issuer-uri}/protocol/openid-connect/token");
        registry.add("spring.security.oauth2.client.registration.admin-api.client-secret",
                () -> KeycloakTestToken.SECRET);
    }

    @AfterAll
    static void stopTheStub() {
        ADMIN_DUBLADO.stop(0);
    }

    @BeforeEach
    void setUpClient() {
        this.comPool = clienteComPoolCompartilhado();
    }

    @Test
    @Timeout(value = LIMITE_SEGUNDOS + 60, unit = TimeUnit.SECONDS)
    void givenManyShortRelayedResponses_whenTheySharePoolAndThreads_thenNoneComesBackCorrupted()
            throws InterruptedException {
        // Given o endereço assinado que o player usaria
        final var assinado = this.links.signedPathFor(UM_VIDEO, "VIDEO");
        final var falhas = new ConcurrentHashMap<String, AtomicLong>();
        final var feitas = new AtomicLong();

        // When a mesma mídia é repassada muitas vezes, por threads que dividem o pool
        martelar(falhas, feitas, () -> baixarPeloSelo(assinado));

        // Then nenhuma resposta volta corrompida
        anotar("repasse em fluxo", falhas, feitas.get());
        assertEquals(Map.of(), resumo(falhas), relatorio(falhas, feitas.get()));
    }

    // **O controle do experimento, na mesma rota.** O selo vai forjado, então a rota responde 403 e
    // volta **sem** repasse: nada de `StreamingResponseBody`, nada de despacho assíncrono.
    //
    // Escolhi este controle em vez de outra rota de propósito. Mesmo cliente, mesmo pool, mesmo
    // conector do Tomcat, mesmo método do controller — a única diferença entre os dois testes é o
    // assíncrono. Um controle em rota diferente trocaria várias coisas de uma vez, e não separaria
    // nada.
    //
    // É o que distingue "o repasse em fluxo voltou limpo" de "este ambiente não quebra nada hoje":
    // com o controle limpo e o repasse limpo, a comparação tem valor; com os dois sujos, o problema
    // é do ambiente, e a conclusão seria outra.
    @Test
    @Timeout(value = LIMITE_SEGUNDOS + 60, unit = TimeUnit.SECONDS)
    void givenTheSameLoadOnASynchronousRoute_whenItSharesPoolAndThreads_thenCompareWithTheRelay()
            throws InterruptedException {
        // Given o mesmo endereço, com a assinatura mexida: ela é o último parâmetro, então um
        // caractere a mais a invalida sem mudar o resto da URL
        final var forjado = this.links.signedPathFor(UM_VIDEO, "VIDEO") + "x";
        final var falhas = new ConcurrentHashMap<String, AtomicLong>();
        final var feitas = new AtomicLong();

        martelar(falhas, feitas, () -> recusaPeloSelo(forjado));

        anotar("controle sincrono", falhas, feitas.get());
        assertEquals(Map.of(), resumo(falhas), relatorio(falhas, feitas.get()));
    }

    // `exchange`, e não `retrieve`: o 403 é o resultado **esperado** aqui, e o tratamento de erro
    // padrão o transformaria em exceção, fazendo o controle contar falha em todo pedido. Status
    // diferente de 403 é que é falha — inclusive 200, que significaria selo forjado aceito.
    private void recusaPeloSelo(final String forjado) {
        final var status = this.comPool
                .get()
                .uri(forjado)
                .exchange((pedido, resposta) -> resposta.getStatusCode().value());
        if (status == null || status != HttpStatus.FORBIDDEN.value()) {
            throw new IllegalStateException("a rota respondeu " + status + " para um selo forjado");
        }
    }

    // O tamanho é conferido junto: resposta truncada que não estoura exceção também é corrupção, e
    // sem esta conta ela passaria por sucesso. É o caso que mais importa aqui — um repasse que
    // perdesse bytes no meio devolveria 200 com vídeo quebrado.
    private void baixarPeloSelo(final String assinado) {
        final var corpo = this.comPool.get().uri(assinado).retrieve().body(byte[].class);
        if (corpo == null || corpo.length != CONTEUDO.length) {
            throw new IllegalStateException("corpo com tamanho inesperado: " + (corpo == null ? -1 : corpo.length));
        }
    }

    // **O pool compartilhado é o instrumento, não um detalhe.** Com um cliente por thread cada uma
    // teria conexão própria, e a corrida entre devolver a conexão e terminar a resposta deixaria de
    // acontecer. `maxTotal` abaixo do número de threads força o reaproveitamento.
    private RestClient clienteComPoolCompartilhado() {
        final var pool = new PoolingHttpClientConnectionManager();
        pool.setMaxTotal(Math.max(2, THREADS / 2));
        pool.setDefaultMaxPerRoute(Math.max(2, THREADS / 2));
        final var http = HttpClients.custom().setConnectionManager(pool).build();
        return RestClient.builder()
                // Com o `/api` do context-path: o endereço que o `MediaLinks` assina é relativo a
                // ele, como o player o recebe.
                .baseUrl("http://localhost:%d/api".formatted(this.port))
                .requestFactory(new HttpComponentsClientHttpRequestFactory(http))
                .build();
    }

    // O dublê do admin, com executor próprio: no padrão do `HttpServer` os handlers correm na thread
    // de despacho, e aí o dublê — e não o catálogo — seria o gargalo da medição.
    private static HttpServer umAdminQueServeORepasse() {
        try {
            final var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.setExecutor(Executors.newCachedThreadPool());
            server.createContext("/videos", exchange -> {
                exchange.getResponseHeaders().add(HttpHeaders.CONTENT_TYPE, "image/png");
                exchange.getResponseHeaders()
                        .add(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"arte.png\"");
                exchange.sendResponseHeaders(200, CONTEUDO.length);
                try (var corpo = exchange.getResponseBody()) {
                    corpo.write(CONTEUDO);
                }
            });
            server.start();
            return server;
        } catch (final IOException naoSubiu) {
            throw new UncheckedIOException("o admin dublado não subiu", naoSubiu);
        }
    }

    private void martelar(final Map<String, AtomicLong> falhas, final AtomicLong feitas, final Runnable pedido)
            throws InterruptedException {
        final var largada = new CountDownLatch(1);
        try (var piscina = Executors.newFixedThreadPool(THREADS)) {
            for (var t = 0; t < THREADS; t++) {
                piscina.submit(() -> {
                    esperar(largada);
                    for (var i = 0; i < POR_THREAD; i++) {
                        umaVez(pedido, falhas, feitas);
                    }
                });
            }
            largada.countDown();
            piscina.shutdown();
            if (!piscina.awaitTermination(LIMITE_SEGUNDOS, TimeUnit.SECONDS)) {
                throw new AssertionError("as threads não terminaram em " + LIMITE_SEGUNDOS + "s");
            }
        }
    }

    // Conta a exceção pela classe, e não pela mensagem: a mensagem traz offsets e bytes que mudam a
    // cada falha, e agrupá-las por ela daria uma linha por ocorrência em vez de um padrão.
    private void umaVez(final Runnable pedido, final Map<String, AtomicLong> falhas, final AtomicLong feitas) {
        try {
            pedido.run();
        } catch (final RuntimeException falha) {
            final var causa = causaRaiz(falha);
            contar(falhas, causa.getClass().getSimpleName());
            // Um exemplo por classe, e não todos: a mensagem é o que nomeou a causa do defeito na
            // admin-api — sem ela, "ParseException" sozinho não diz quais bytes estavam fora de
            // lugar. Guardar todas daria uma linha por ocorrência e esconderia o padrão.
            this.exemplos.putIfAbsent(causa.getClass().getSimpleName(), String.valueOf(causa.getMessage()));
        } finally {
            feitas.incrementAndGet();
        }
    }

    private static Throwable causaRaiz(final Throwable falha) {
        var causa = falha;
        while (causa.getCause() != null) {
            causa = causa.getCause();
        }
        return causa;
    }

    private static void contar(final Map<String, AtomicLong> falhas, final String chave) {
        falhas.computeIfAbsent(chave, qual -> new AtomicLong()).incrementAndGet();
    }

    private static Map<String, Long> resumo(final Map<String, AtomicLong> falhas) {
        return falhas.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entrada -> entrada.getValue().get()));
    }

    // **O resultado é registrado inclusive quando passa.** O irmão na admin-api só relata na falha,
    // e isso me custou tempo agora: "passou" não diz quantos pedidos foram, e um volume que caiu por
    // acidente — propriedade não repassada, por exemplo — passaria igual, sem nada no registro para
    // desconfiar. O número medido é a saída deste teste, não um efeito colateral dele.
    private void anotar(final String arco, final Map<String, AtomicLong> falhas, final long feitas) {
        System.out.println("[estresse] " + arco + ": " + relatorio(falhas, feitas));
        this.exemplos.forEach((classe, mensagem) ->
                System.out.println("[estresse]   exemplo de " + classe + ": " + mensagem));
    }

    private static String relatorio(final Map<String, AtomicLong> falhas, final long feitas) {
        final var total = falhas.values().stream().mapToLong(AtomicLong::get).sum();
        return "%d falha(s) em %d pedidos (%.2f%%), com %d thread(s) e pool compartilhado: %s"
                .formatted(total, feitas, feitas == 0 ? 0.0 : 100.0 * total / feitas, THREADS, resumo(falhas));
    }

    private static void esperar(final CountDownLatch largada) {
        try {
            largada.await(LIMITE_SEGUNDOS, TimeUnit.SECONDS);
        } catch (final InterruptedException interrompida) {
            Thread.currentThread().interrupt();
        }
    }
}
