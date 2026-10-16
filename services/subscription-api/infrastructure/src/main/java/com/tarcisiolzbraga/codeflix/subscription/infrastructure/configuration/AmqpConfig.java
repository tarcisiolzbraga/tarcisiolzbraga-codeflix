package com.tarcisiolzbraga.codeflix.subscription.infrastructure.configuration;

import com.tarcisiolzbraga.codeflix.subscription.domain.account.signup.SignUpNotifier;
import com.tarcisiolzbraga.codeflix.subscription.infrastructure.messaging.AmqpProperties;
import com.tarcisiolzbraga.codeflix.subscription.infrastructure.messaging.RabbitSignUpNotifier;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.core.RabbitOperations;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.ObjectMapper;

// A topologia é declarada aqui, de forma idempotente, e criada na subida da aplicação. Isso é o que
// a mantém funcionando contra um broker vazio, como o de Testcontainers nos testes.
//
// A MESMA topologia está em provisioning/rabbitmq/definitions.json, que o broker do docker compose
// carrega no boot. Mudança de exchange, fila, binding ou routing key tem de ser feita nos dois
// lugares: se divergirem, a aplicação falha com PRECONDITION_FAILED ao declarar algo diferente do
// que existe. É a mesma regra da admin-api.
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(AmqpProperties.class)
public class AmqpConfig {

    @Bean
    DirectExchange mailEventsExchange(final AmqpProperties properties) {
        return new DirectExchange(properties.exchange());
    }

    @Bean
    Queue signUpValidationQueue(final AmqpProperties properties) {
        return new Queue(properties.queues().signUpValidation().queue());
    }

    @Bean
    Binding signUpValidationBinding(
            final AmqpProperties properties, final DirectExchange mailEventsExchange, final Queue signUpValidationQueue) {
        return BindingBuilder.bind(signUpValidationQueue)
                .to(mailEventsExchange)
                .with(properties.queues().signUpValidation().routingKey());
    }

    @Bean
    SignUpNotifier signUpNotifier(
            final AmqpProperties properties, final RabbitOperations operations, final ObjectMapper mapper) {
        return new RabbitSignUpNotifier(properties, operations, mapper);
    }
}
