package com.tarcisiolzbraga.codeflix.subscription.infrastructure.job;

import com.tarcisiolzbraga.codeflix.subscription.domain.account.signup.PendingSignUpGateway;
import com.tarcisiolzbraga.codeflix.subscription.infrastructure.configuration.SignUpProperties;
import java.time.Clock;
import java.time.Duration;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

// Apaga o cadastro pendente que já não serve a ninguém.
//
// **Isto é retenção de dado pessoal, e não limpeza de tabela.** A `pending_sign_up` guarda e-mail,
// nome completo e número de documento de quem **pediu** cadastro — inclusive de quem nunca
// confirmou, e portanto nunca virou cliente nem concordou com coisa alguma. Antes deste job, só
// saía a linha de quem pedisse o cadastro de novo com o mesmo e-mail: todo o resto ficava para
// sempre.
//
// O prazo e as duas condições estão explicados no `PendingSignUpGateway`; o resumo é que a janela
// preserva o 422 de "o link venceu" em vez de o trocar por 404 na hora do vencimento.
//
// **Duas instâncias rodam isto duas vezes**, e é inofensivo: o DELETE é por limiar de tempo, então a
// segunda passagem não acha mais nada. Não há lock, pela mesma razão que o job de cobrança não tem.
@Component
public class ExpiredPendingSignUpsJob {

    private static final Logger LOG = LoggerFactory.getLogger(ExpiredPendingSignUpsJob.class);

    private final PendingSignUpGateway pendingSignUps;
    private final Clock clock;
    private final Duration retention;

    public ExpiredPendingSignUpsJob(
            final PendingSignUpGateway pendingSignUps, final Clock clock, final SignUpProperties properties) {
        this.pendingSignUps = Objects.requireNonNull(pendingSignUps, "'pendingSignUps' should not be null");
        this.clock = Objects.requireNonNull(clock, "'clock' should not be null");
        this.retention = properties.retention();
    }

    // De madrugada, como a varredura de cobrança: o trabalho é de banco e não disputa com o pico.
    @Scheduled(cron = "${signup.cleanup-job.cron}", zone = "UTC")
    public int removeExpired() {
        final var threshold = this.clock.instant().minus(this.retention);
        final var removed = this.pendingSignUps.deleteExpiredBefore(threshold);
        // Em INFO porque é o número que prova que a retenção está acontecendo. Zero também informa.
        LOG.info("cadastros pendentes vencidos apagados: {} (limiar {})", removed, threshold);
        return removed;
    }
}
