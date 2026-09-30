package com.tarcisiolzbraga.codeflix.admin.infrastructure.configuration;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

// Um exchange direto e uma fila para cada sentido: video-created sai daqui quando um arquivo é
// enviado, video-encoded volta com o resultado do codificador.
@ConfigurationProperties("amqp")
public record AmqpProperties(String exchange, Queues queues, Outbox outbox) {

    public record Queues(Binding videoCreated, Binding videoEncoded) {
    }

    // De quanto em quanto tempo o relay procura o que entregar, quantas linhas leva por vez, e
    // por quanto tempo a linha entregue fica guardada antes de ser apagada.
    public record Outbox(long pollInterval, int batchSize, long cleanupInterval, Duration retention) {
    }

    public record Binding(String queue, String routingKey) {
    }
}
