package com.tarcisiolzbraga.codeflix.subscription.infrastructure.ratelimit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tarcisiolzbraga.codeflix.subscription.infrastructure.E2ETest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.client.RestClient;

// @E2ETest porque o limitador é um filtro de servlet: a fatia @WebMvcTest não monta a cadeia de
// filtros registrada por FilterRegistrationBean, então um teste dali passaria sem exercitar nada.
//
// Os tetos são apertados a 2 e 3 de propósito, e é o que torna o teste rápido e determinístico: o
// que está sob teste é o filtro recusar ao passar do teto e os dois degraus serem independentes —
// não os números escolhidos para produção, que são configuração.
@E2ETest
@TestPropertySource(
        properties = {
            "management.server.port=0",
            "rate-limit.sign-up.capacity=2",
            "rate-limit.sign-up.period=10m",
            "rate-limit.confirm.capacity=4",
            "rate-limit.confirm.period=10m",
            "rate-limit.general.capacity=3",
            "rate-limit.general.period=10m"
        })
class RateLimitE2ETest {

    private static final String SIGN_UP = "/accounts/sign-up";
    private static final String CONFIRM = "/accounts/sign-up/confirm";
    private static final String OPEN_CATALOG = "/plans/on-sale";

    @LocalServerPort
    private int port;

    @Value("${local.management.port}")
    private int managementPort;

    @Test
    void givenTheSignUpTierExhausted_whenCallItAgain_thenRefuseWithTooManyRequests() {
        exhaust(SIGN_UP, 2);

        final var actual = post(SIGN_UP);

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, actual.getStatusCode());
    }

    // O corpo é o ApiError do serviço, e não o erro padrão do container: o filtro responde antes de
    // o Spring MVC entrar na requisição, então quem escreve o JSON é ele.
    @Test
    void givenTheSignUpTierExhausted_whenCallItAgain_thenAnswerApiErrorAndRetryAfter() {
        exhaust(SIGN_UP, 2);

        final var actual = post(SIGN_UP);

        assertTrue(actual.getBody().contains("\"message\""), actual.getBody());
        final var retryAfter = actual.getHeaders().getFirst(HttpHeaders.RETRY_AFTER);
        assertNotNull(retryAfter, "faltou o Retry-After");
        assertTrue(Integer.parseInt(retryAfter) > 0, retryAfter);
    }

    // Os dois degraus têm de ser independentes: com um contador só, o cadastro gastaria fichas de
    // quem está lendo o catálogo, e vice-versa.
    @Test
    void givenTheSignUpTierExhausted_whenCallAGeneralRoute_thenStillAnswerIt() {
        exhaust(SIGN_UP, 2);

        final var actual = statusOf(OPEN_CATALOG);

        assertNotEquals(HttpStatus.TOO_MANY_REQUESTS, actual);
    }

    // Confirmar tem degrau próprio, mais folgado: com balde único, quem se cadastra e clica no
    // link gastaria do mesmo saldo, e quanto cabe passaria a depender da mistura das duas rotas.
    @Test
    void givenTheSignUpTierExhausted_whenConfirms_thenStillAnswerIt() {
        exhaust(SIGN_UP, 2);

        final var actual = post(CONFIRM);

        assertNotEquals(HttpStatus.TOO_MANY_REQUESTS, actual.getStatusCode());
    }

    // A porta de gerenciamento fica fora do limite: as sondas batem de dez em dez segundos, e
    // limitá-las faria o orquestrador declarar morto um pod perfeitamente vivo.
    @Test
    void givenManyProbes_whenTheyExceedEveryTier_thenNeverRefuseThem() {
        final var client = RestClient.builder().baseUrl("http://localhost:" + this.managementPort).build();

        for (var i = 0; i < 10; i++) {
            final var actual = client.get()
                    .uri("/actuator/health/readiness")
                    .exchange((request, response) -> response.getStatusCode(), false);
            assertEquals(HttpStatus.OK, actual, "a sonda foi limitada na tentativa " + i);
        }
    }

    private void exhaust(final String uri, final int capacity) {
        for (var i = 0; i < capacity; i++) {
            post(uri);
        }
    }

    private ResponseEntity<String> post(final String uri) {
        return client(this.port)
                .post()
                .uri(uri)
                .contentType(MediaType.APPLICATION_JSON)
                .body("{}")
                .exchange((request, response) -> ResponseEntity.status(response.getStatusCode())
                        .headers(response.getHeaders())
                        .body(new String(response.getBody().readAllBytes())), false);
    }

    private HttpStatusCode statusOf(final String uri) {
        return client(this.port)
                .get()
                .uri(uri)
                .exchange((request, response) -> response.getStatusCode(), false);
    }

    private RestClient client(final int targetPort) {
        return RestClient.builder().baseUrl("http://localhost:" + targetPort).build();
    }
}
