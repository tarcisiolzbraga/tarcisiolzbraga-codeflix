package com.tarcisiolzbraga.codeflix.subscription.infrastructure.messaging;

import com.tarcisiolzbraga.codeflix.subscription.infrastructure.configuration.ConfiguredValue;
import org.springframework.boot.context.properties.ConfigurationProperties;

// A topologia do que sai deste serviço, e onde a pessoa confirma o cadastro.
//
// A URL de confirmação fica aqui, e não no serviço de e-mail: ela depende de onde esta aplicação
// está publicada, que é coisa que só este lado sabe. O serviço de e-mail recebe o link pronto e não
// precisa conhecer rota nenhuma daqui.
@ConfigurationProperties("amqp")
public record AmqpProperties(String exchange, Queues queues, String confirmationUrl) {

    public AmqpProperties {
        ConfiguredValue.text("amqp.exchange", exchange);
        ConfiguredValue.url("amqp.confirmation-url", confirmationUrl);
    }

    public record Queues(Destination signUpValidation) {}

    public record Destination(String queue, String routingKey) {}
}
