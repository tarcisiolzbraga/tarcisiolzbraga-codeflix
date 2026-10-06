package com.tarcisiolzbraga.codeflix.admin.infrastructure.messaging;

import com.tarcisiolzbraga.codeflix.admin.domain.util.InstantUtils;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.configuration.AmqpProperties;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.messaging.persistence.OutboxEventRepository;
import java.time.Duration;
import java.util.Objects;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

// A tabela de saída é fila, não histórico: a linha entregue fica um tempo para consulta e depois
// sai. O que aconteceu com o vídeo continua na auditoria dele. Linha pendente nunca é apagada,
// por mais antiga que seja — ela ainda tem uma entrega a fazer.
@Component
public class OutboxCleaner {

    private final OutboxEventRepository outboxRepository;
    private final Duration retention;

    public OutboxCleaner(final OutboxEventRepository outboxRepository, final AmqpProperties properties) {
        this.outboxRepository = Objects.requireNonNull(outboxRepository, "'outboxRepository' should not be null");
        this.retention = properties.outbox().retention();
    }

    @Scheduled(
            fixedDelayString = "${amqp.outbox.cleanup-interval}",
            initialDelayString = "${amqp.outbox.cleanup-interval}")
    @Transactional
    public int removeDelivered() {
        return this.outboxRepository.deleteDeliveredBefore(InstantUtils.now().minus(this.retention));
    }
}
