package com.tarcisiolzbraga.codeflix.admin.infrastructure.messaging;

import com.tarcisiolzbraga.codeflix.admin.domain.util.InstantUtils;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.configuration.AmqpProperties;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.messaging.persistence.OutboxEventJpaEntity;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.messaging.persistence.OutboxEventRepository;
import java.util.Objects;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

// Tira da tabela de saída o que ainda não foi entregue e manda ao broker. Falhar aqui não perde
// nada: a linha continua sem sent_at e a próxima passagem tenta de novo. Em compensação, uma queda
// entre o envio e a marcação sai duas vezes — por isso o consumidor precisa tolerar repetição.
//
// Uma transação por linha, não por lote: assim uma falha no meio não desfaz a marcação do que já
// saiu, que seria reenviado sem necessidade. A falha interrompe a passagem, preservando a ordem de
// entrega, e a seguinte retoma da linha que falhou.
//
// Vale para uma instância só. Rodando várias, duas leriam as mesmas linhas e entregariam em
// dobro; o passo seguinte seria travar a leitura com SKIP LOCKED.
@Component
public class OutboxRelay {

    private final OutboxEventRepository outboxRepository;
    private final RabbitEventSender sender;
    private final int batchSize;

    public OutboxRelay(
            final OutboxEventRepository outboxRepository,
            final RabbitEventSender sender,
            final AmqpProperties properties) {
        this.outboxRepository = Objects.requireNonNull(outboxRepository, "'outboxRepository' should not be null");
        this.sender = Objects.requireNonNull(sender, "'sender' should not be null");
        this.batchSize = properties.outbox().batchSize();
    }

    // initialDelay junto do fixedDelay para o relógio só começar a contar depois da subida: sem
    // isso a primeira passagem sai no instante em que o contexto abre.
    @Scheduled(
            fixedDelayString = "${amqp.outbox.poll-interval}",
            initialDelayString = "${amqp.outbox.poll-interval}")
    public void deliverPending() {
        this.outboxRepository
                .findBySentAtIsNullOrderByCreatedAtAsc(PageRequest.of(0, this.batchSize))
                .forEach(this::deliver);
    }

    // Sem transação em volta do método, o save do repositório abre a sua, uma por linha. A linha
    // vem desligada da leitura, então o save faz um merge: custa um select a mais, e em troca cada
    // entrega é confirmada por conta própria.
    private void deliver(final OutboxEventJpaEntity row) {
        this.sender.send(row.getRoutingKey(), row.getPayload());
        row.markSent(InstantUtils.now());
        this.outboxRepository.save(row);
    }
}
