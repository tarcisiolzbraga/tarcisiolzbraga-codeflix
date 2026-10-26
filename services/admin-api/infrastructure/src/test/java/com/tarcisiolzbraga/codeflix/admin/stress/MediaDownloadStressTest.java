package com.tarcisiolzbraga.codeflix.admin.stress;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.tarcisiolzbraga.codeflix.admin.e2e.CategoryE2EDsl;
import com.tarcisiolzbraga.codeflix.admin.e2e.E2EMediaFile;
import com.tarcisiolzbraga.codeflix.admin.e2e.VideoE2EDsl;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.KeycloakTestToken;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

// Mede a taxa de falha da rota de mídia sob muitas respostas curtas em sequência.
//
// **Por que isto existe.** A intermitência do fluxo de mídia está medida: cerca de 1 commit em 4 no
// CI, com dois SHAs em que execuções simultâneas do mesmo código divergiram. O que faltava era um
// instrumento que a reproduzisse **sob demanda** — sem ele, "o CI ficou verde" não prova conserto
// nenhum, e o README da admin-api já registra quatro builds inteiros verdes convivendo com CI
// vermelho.
//
// **A condição de reprodução, aprendida da forma difícil.** O registro do commit que consertou a
// drenagem diz: a classe isolada rodou 8 vezes verde, e só o **build inteiro** falhava, porque lá o
// Tomcat e o pool de conexões são compartilhados entre classes. Então este teste não repete um
// cenário — ele concentra a condição: um pool só, reaproveitado por várias threads, com respostas
// curtas em sequência.
//
// **O cliente é o mesmo da falha.** A exceção aparece como `ParseException` do Apache HttpClient 5,
// que é o parser dele reclamando de bytes fora de lugar no socket. Medir com outro cliente mediria
// outra coisa: um gerador de carga em Go toleraria ou relataria diferente, e o sintoma documentado
// se perderia.
//
// A asserção é de **zero falha**, e não de uma taxa aceitável: resposta corrompida não tem cota. Se
// falhar, a mensagem traz a taxa e a quebra por exceção — que é o dado que faltava.
@StressTest
class MediaDownloadStressTest implements VideoE2EDsl, CategoryE2EDsl {

    // Pequenos de propósito: a hipótese que sobrou no README fala de respostas assíncronas **curtas**
    // em sequência. Arquivo grande daria outro regime, com menos trocas de resposta por segundo.
    private static final String CONTEUDO = "imagem pequena, de propósito";
    private static final int THREADS = Integer.getInteger("stress.threads", 8);
    private static final int POR_THREAD = Integer.getInteger("stress.iteracoes", 150);
    private static final long LIMITE_SEGUNDOS = 300;

    @Value("${local.server.port}")
    private int port;

    @Autowired
    private KeycloakTestToken token;

    private RestClient client;
    private RestClient comPool;

    @BeforeEach
    void setUpClients() {
        this.client = RestClient.builder()
                .baseUrl("http://localhost:" + this.port)
                .defaultHeader(HttpHeaders.AUTHORIZATION, this.token.bearer())
                .build();
        this.comPool = clienteComPoolCompartilhado();
    }

    @Override
    public RestClient client() {
        return this.client;
    }

    @Test
    void givenManyShortMediaResponses_whenTheySharePoolAndThreads_thenNoneComesBackCorrupted()
            throws InterruptedException {
        final var id = givenAVideo("Duna", givenACategory("Filmes", null));
        uploadMedia(id, "BANNER", E2EMediaFile.of("arte.png", "image/png", CONTEUDO));
        final var falhas = new ConcurrentHashMap<String, AtomicLong>();
        final var feitas = new AtomicLong();

        martelar(falhas, feitas, () -> baixarMidia(id));

        assertEquals(Map.of(), resumo(falhas), relatorio(falhas, feitas.get()));
    }

    private void martelar(
            final Map<String, AtomicLong> falhas, final AtomicLong feitas, final Runnable pedido)
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
            contar(falhas, nomeDaCausaRaiz(falha));
        } finally {
            feitas.incrementAndGet();
        }
    }

    // O tamanho é conferido junto: resposta truncada que não estoura exceção também é corrupção, e
    // sem esta conta ela passaria por sucesso.
    private void baixarMidia(final String id) {
        final var corpo = this.comPool.get()
                .uri("/videos/{id}/medias/{type}", id, "BANNER")
                .retrieve()
                .body(byte[].class);
        if (corpo == null || corpo.length != CONTEUDO.getBytes().length) {
            throw new IllegalStateException("corpo com tamanho inesperado: " + (corpo == null ? -1 : corpo.length));
        }
    }

    // **O pool compartilhado é o instrumento, não um detalhe.** Com um cliente por thread cada uma
    // teria conexão própria, e a corrida entre devolver a conexão e terminar a resposta — que é a
    // hipótese — deixaria de acontecer. `maxTotal` abaixo do número de threads força o reaproveitamento.
    private RestClient clienteComPoolCompartilhado() {
        final var pool = new PoolingHttpClientConnectionManager();
        pool.setMaxTotal(Math.max(2, THREADS / 2));
        pool.setDefaultMaxPerRoute(Math.max(2, THREADS / 2));
        final var http = HttpClients.custom().setConnectionManager(pool).build();
        return RestClient.builder()
                .baseUrl("http://localhost:" + this.port)
                .defaultHeader(HttpHeaders.AUTHORIZATION, this.token.bearer())
                .requestFactory(new HttpComponentsClientHttpRequestFactory(http))
                .build();
    }

    private static String nomeDaCausaRaiz(final Throwable falha) {
        var causa = falha;
        while (causa.getCause() != null) {
            causa = causa.getCause();
        }
        return causa.getClass().getSimpleName();
    }

    private static void contar(final Map<String, AtomicLong> falhas, final String chave) {
        falhas.computeIfAbsent(chave, qual -> new AtomicLong()).incrementAndGet();
    }

    private static Map<String, Long> resumo(final Map<String, AtomicLong> falhas) {
        return falhas.entrySet().stream()
                .collect(java.util.stream.Collectors.toMap(Map.Entry::getKey, e -> e.getValue().get()));
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

    // **O controle do experimento.** A rota de mídia responde por `StreamingResponseBody`, então o
    // Spring a despacha de forma assíncrona; a hipótese que o README carrega desde 19/10 é que são
    // "muitas respostas assíncronas curtas em sequência, no mesmo pool".
    //
    // Este teste martela uma rota **síncrona** — o `GET /videos/{id}`, JSON comum — com o mesmo pool,
    // as mesmas threads e o mesmo volume. É o que separa duas conclusões muito diferentes:
    //
    // - controle limpo  -> o despacho assíncrono está implicado, e o experimento de declarar o
    //                      caminho sem `Range` como síncrono vale o preço que tem;
    // - controle falha  -> a hipótese está errada, e o problema é da combinação cliente/pool/Tomcat
    //                      em geral, não do assíncrono. Dez dias de suspeita cairiam aqui.
    @Test
    void givenTheSameLoadOnASynchronousRoute_whenItSharesPoolAndThreads_thenCompareWithTheMediaRoute()
            throws InterruptedException {
        final var id = givenAVideo("Duna", givenACategory("Filmes", null));
        final var falhas = new ConcurrentHashMap<String, AtomicLong>();
        final var feitas = new AtomicLong();

        martelar(falhas, feitas, () -> this.comPool.get()
                .uri("/videos/{id}", id)
                .retrieve()
                .body(byte[].class));

        assertEquals(Map.of(), resumo(falhas), relatorio(falhas, feitas.get()));
    }
}
