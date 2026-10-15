package com.tarcisiolzbraga.codeflix.subscription.infrastructure;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Objects;

// O relógio do sistema mais um deslocamento que a jornada move, para a cobrança poder esperar o
// vencimento chegar: a assinatura nasce vencendo em um mês.
//
// Sem ele não haveria como exercitar cobrança sobre a API de verdade. A alternativa seria mexer na
// data direto no banco, e aí a jornada afirmaria sobre um estado que a API não produziu.
//
// Deslocamento, e não instante congelado, de propósito: enquanto ninguém adianta nada ele é
// indistinguível do relógio do sistema, então entrar no contexto de todas as jornadas não muda o
// comportamento de nenhuma.
public class AdvanceableClock extends Clock {

    private final Clock base;
    private volatile Duration offset;

    private AdvanceableClock(final Clock base, final Duration offset) {
        this.base = Objects.requireNonNull(base, "'base' should not be null");
        this.offset = Objects.requireNonNull(offset, "'offset' should not be null");
    }

    public static AdvanceableClock overTheSystemClock() {
        return new AdvanceableClock(Clock.systemUTC(), Duration.ZERO);
    }

    public void advanceBy(final Duration amount) {
        this.offset = this.offset.plus(amount);
    }

    // Chamado antes de cada teste. O bean é único no contexto, então sem isto o deslocamento de um
    // teste sobraria para o seguinte e a suíte passaria a depender da ordem — descoberto com um
    // "esperava incomplete, veio canceled" que só aparecia quando outro teste rodava antes.
    public void reset() {
        this.offset = Duration.ZERO;
    }

    @Override
    public ZoneId getZone() {
        return this.base.getZone();
    }

    @Override
    public Clock withZone(final ZoneId aZone) {
        return new AdvanceableClock(this.base.withZone(aZone), this.offset);
    }

    @Override
    public Instant instant() {
        return this.base.instant().plus(this.offset);
    }
}
