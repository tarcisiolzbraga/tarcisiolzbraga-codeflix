package com.tarcisiolzbraga.codeflix.admin.infrastructure.messaging;

import com.tarcisiolzbraga.codeflix.admin.domain.util.InstantUtils;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.configuration.AmqpProperties;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.messaging.persistence.OutboxEventJpaEntity;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.messaging.persistence.OutboxEventRepository;
import java.util.Objects;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

// Tira da tabela de saída o que ainda não foi entregue e manda ao broker. Falhar aqui não perde
// nada: a linha continua sem sent_at e a próxima passagem tenta de novo. Em compensação, uma queda
// entre o envio e a marcação sai duas vezes — por isso o consumidor precisa tolerar repetição.
//
// **Vale para mais de uma instância**, e é o `SKIP LOCKED` do lockNextPending que garante isso: cada
// passagem trava a linha que vai entregar, e a instância vizinha salta o que está travado em vez de
// ler a mesma linha. Antes disto o relay pressupunha instância única, e duas entregariam em dobro.
//
// Uma transação por linha, não por lote: assim uma falha no meio não desfaz a marcação do que já
// saiu, que seria reenviado sem necessidade. O lote virou um laço de transações curtas, e não um
// `findBy...` paginado, justamente porque o lock de uma linha vale até o fim da transação dela —
// travar cinquenta de uma vez seguraria quarenta e nove linhas durante quarenta e nove envios.
//
// A `TransactionTemplate` existe por um motivo chato e real: `@Transactional` num método chamado de
// dentro da própria classe não passa pelo proxy e não abre transação nenhuma. Sem transação não há
// lock, e sem lock o SKIP LOCKED não protege de nada — falharia calado, que é o pior modo.
//
// **O envio acontece com o lock na mão**, e isso é escolha entre dois males. Marcar e depois enviar
// soltaria o lock antes, mas uma queda entre as duas coisas perderia a mensagem para sempre; enviar
// sob o lock troca isso por entrega repetida, que o consumidor já tolera. O preço é uma linha
// travada durante uma chamada de rede, e o teto dela é o tempo do broker.
//
// **A ordem de entrega entre instâncias não é preservada**, e não era antes tampouco: o encoder
// consome a mesma fila com vários operários em paralelo (`ENCODER_WORKERS`, padrão 2), então
// mensagem que sai em ordem já era processada fora de ordem. O que se perde aqui é uma garantia que
// o sistema nunca teve ponta a ponta.
@Component
public class OutboxRelay {

    private final OutboxEventRepository outboxRepository;
    private final RabbitEventSender sender;
    private final TransactionTemplate transactions;
    private final int batchSize;

    public OutboxRelay(
            final OutboxEventRepository outboxRepository,
            final RabbitEventSender sender,
            final TransactionTemplate transactions,
            final AmqpProperties properties) {
        this.outboxRepository = Objects.requireNonNull(outboxRepository, "'outboxRepository' should not be null");
        this.sender = Objects.requireNonNull(sender, "'sender' should not be null");
        this.transactions = Objects.requireNonNull(transactions, "'transactions' should not be null");
        this.batchSize = properties.outbox().batchSize();
    }

    // initialDelay junto do fixedDelay para o relógio só começar a contar depois da subida: sem
    // isso a primeira passagem sai no instante em que o contexto abre.
    //
    // O batchSize agora é o teto de entregas por passagem, e não o tamanho de uma leitura: a
    // passagem para quando não há mais nada pendente que esta instância consiga travar.
    @Scheduled(
            fixedDelayString = "${amqp.outbox.poll-interval}",
            initialDelayString = "${amqp.outbox.poll-interval}")
    public void deliverPending() {
        for (var delivered = 0; delivered < this.batchSize; delivered++) {
            if (!deliverNext()) {
                return;
            }
        }
    }

    // Devolve false quando não havia o que entregar, para o laço acima parar. A entidade vem
    // gerenciada pela transação, então marcar o envio basta: o flush do commit grava.
    private boolean deliverNext() {
        return Boolean.TRUE.equals(this.transactions.execute(status -> this.outboxRepository
                .lockNextPending()
                .map(this::deliver)
                .orElse(false)));
    }

    private boolean deliver(final OutboxEventJpaEntity row) {
        this.sender.send(row.getRoutingKey(), row.getPayload());
        row.markSent(InstantUtils.now());
        return true;
    }
}
