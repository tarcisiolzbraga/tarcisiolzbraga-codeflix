package com.tarcisiolzbraga.codeflix.admin.infrastructure.messaging;

import java.util.Objects;
import org.springframework.amqp.rabbit.core.RabbitOperations;

// A metade que só fala com o broker: recebe para onde vai e o que vai, e entrega. Não conhece
// evento de domínio, o que permite ao relay entregar uma linha da tabela de saída sem
// reconstruir o objeto que a originou.
public class RabbitEventSender {

    private final String exchange;
    private final RabbitOperations operations;

    public RabbitEventSender(final String exchange, final RabbitOperations operations) {
        this.exchange = Objects.requireNonNull(exchange, "'exchange' should not be null");
        this.operations = Objects.requireNonNull(operations, "'operations' should not be null");
    }

    public void send(final String routingKey, final String payload) {
        this.operations.convertAndSend(this.exchange, routingKey, payload);
    }
}
