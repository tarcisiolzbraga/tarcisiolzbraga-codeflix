package com.tarcisiolzbraga.codeflix.subscription.infrastructure.keycloak;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

// O token de credenciais de cliente vale minutos. A referência mantém um job que o renova de tempo
// em tempo; aqui ele é buscado sob demanda e guardado até perto de vencer, o que dispensa a tarefa
// periódica — e uma tarefa que morre em silêncio deixaria de renovar sem ninguém notar.
class ClientCredentialsTest {

    private static final Instant NOW = Instant.parse("2026-10-14T12:00:00Z");
    private static final Duration MARGIN = Duration.ofSeconds(30);

    @Test
    void givenNoTokenYet_whenAsksForOne_thenFetchesIt() {
        final var source = new CountingSource(Duration.ofMinutes(5));
        final var credentials = credentialsOf(source, NOW);

        final var actual = credentials.bearer();

        assertEquals("Bearer token-1", actual);
    }

    @Test
    void givenATokenStillFarFromExpiring_whenAsksAgain_thenReusesItWithoutFetching() {
        final var source = new CountingSource(Duration.ofMinutes(5));
        final var credentials = credentialsOf(source, NOW);
        credentials.bearer();

        final var actual = credentials.bearer();

        assertEquals("Bearer token-1", actual);
        assertEquals(1, source.calls());
    }

    // Vencido quer dizer "vence dentro da margem": pedir no último segundo deixaria a requisição
    // sair com um token que vence no caminho.
    @Test
    void givenATokenExpiringWithinTheMargin_whenAsksAgain_thenFetchesANewOne() {
        final var source = new CountingSource(Duration.ofSeconds(20));
        final var credentials = credentialsOf(source, NOW);
        credentials.bearer();

        final var actual = credentials.bearer();

        assertEquals("Bearer token-2", actual);
        assertEquals(2, source.calls());
    }

    @Test
    void givenATokenThatAlreadyExpired_whenAsksAgain_thenFetchesANewOne() {
        final var source = new CountingSource(Duration.ofSeconds(-1));
        final var credentials = credentialsOf(source, NOW);
        credentials.bearer();

        final var actual = credentials.bearer();

        assertEquals("Bearer token-2", actual);
    }

    private static ClientCredentials credentialsOf(final CountingSource source, final Instant now) {
        return new ClientCredentials(source, Clock.fixed(now, ZoneOffset.UTC), MARGIN);
    }

    // Conta as buscas, que é o que distingue reaproveitar de renovar.
    private static final class CountingSource implements ClientCredentials.TokenSource {

        private final Duration lifetime;
        private final AtomicInteger calls = new AtomicInteger();

        private CountingSource(final Duration lifetime) {
            this.lifetime = lifetime;
        }

        @Override
        public ClientCredentials.Token fetch() {
            final var n = this.calls.incrementAndGet();
            return new ClientCredentials.Token("token-" + n, NOW.plus(this.lifetime));
        }

        private int calls() {
            return this.calls.get();
        }
    }
}
