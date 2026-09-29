package com.tarcisiolzbraga.codeflix.admin.infrastructure.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

// Um exchange direto e uma fila para cada sentido: video-created sai daqui quando um arquivo é
// enviado, video-encoded volta com o resultado do codificador.
@ConfigurationProperties("amqp")
public record AmqpProperties(String exchange, Queues queues) {

    public record Queues(Binding videoCreated, Binding videoEncoded) {
    }

    public record Binding(String queue, String routingKey) {
    }
}
