package com.tarcisiolzbraga.codeflix.subscription.infrastructure.keycloak;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

// O token que o serviço apresenta à API administrativa do Keycloak. Buscado sob demanda e guardado
// até perto de vencer.
//
// Sem a tarefa periódica que a referência usa para renová-lo: um job que morre em silêncio deixaria
// de renovar sem ninguém notar, e o primeiro sinal seria 401 em produção. Buscando no uso, a falha
// aparece na requisição que precisava do token, com a causa junto.
//
// A margem existe para o token não vencer no caminho: pedir um que vence em dois segundos faria a
// chamada ao Keycloak sair com credencial morta.
public class ClientCredentials {

    private final TokenSource source;
    private final Clock clock;
    private final Duration margin;

    private Token cached;

    public ClientCredentials(final TokenSource source, final Clock clock, final Duration margin) {
        this.source = Objects.requireNonNull(source, "'source' should not be null");
        this.clock = Objects.requireNonNull(clock, "'clock' should not be null");
        this.margin = Objects.requireNonNull(margin, "'margin' should not be null");
    }

    // synchronized porque virtual threads atendem as requisições em paralelo: sem isto, duas
    // chegando com o token vencido buscariam duas vezes.
    public synchronized String bearer() {
        if (this.cached == null || this.cached.expiresBefore(this.clock.instant().plus(this.margin))) {
            this.cached = Objects.requireNonNull(this.source.fetch(), "the token source returned nothing");
        }
        return "Bearer " + this.cached.value();
    }

    @FunctionalInterface
    public interface TokenSource {

        Token fetch();
    }

    public record Token(String value, Instant expiresAt) {

        public Token {
            Objects.requireNonNull(value, "'value' should not be null");
            Objects.requireNonNull(expiresAt, "'expiresAt' should not be null");
        }

        private boolean expiresBefore(final Instant moment) {
            return this.expiresAt.isBefore(moment);
        }
    }
}
