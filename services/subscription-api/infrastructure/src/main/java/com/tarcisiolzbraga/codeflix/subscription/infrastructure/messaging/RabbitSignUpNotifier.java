package com.tarcisiolzbraga.codeflix.subscription.infrastructure.messaging;

import com.tarcisiolzbraga.codeflix.subscription.domain.account.signup.SignUpNotifier;
import com.tarcisiolzbraga.codeflix.subscription.domain.account.signup.SignUpValidation;
import com.tarcisiolzbraga.codeflix.subscription.domain.exceptions.InternalErrorException;
import com.tarcisiolzbraga.codeflix.subscription.infrastructure.messaging.models.SignUpValidationMessage;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitOperations;
import tools.jackson.databind.ObjectMapper;

// Publica o pedido de validação. Quem entrega é o serviço de e-mail, do outro lado da fila.
public class RabbitSignUpNotifier implements SignUpNotifier {

    private final AmqpProperties properties;
    private final RabbitOperations operations;
    private final ObjectMapper mapper;

    public RabbitSignUpNotifier(
            final AmqpProperties properties, final RabbitOperations operations, final ObjectMapper mapper) {
        this.properties = Objects.requireNonNull(properties, "'properties' should not be null");
        this.operations = Objects.requireNonNull(operations, "'operations' should not be null");
        this.mapper = Objects.requireNonNull(mapper, "'mapper' should not be null");
    }

    @Override
    public void sendValidation(final SignUpValidation aValidation) {
        final var message = new SignUpValidationMessage(
                aValidation.to().value(), aValidation.name().firstname(), confirmationUrlOf(aValidation));
        try {
            this.operations.convertAndSend(
                    this.properties.exchange(),
                    this.properties.queues().signUpValidation().routingKey(),
                    this.mapper.writeValueAsString(message),
                    it -> {
                        it.getMessageProperties().setContentType(MessageProperties.CONTENT_TYPE_JSON);
                        return it;
                    });
        } catch (final AmqpException exception) {
            // Sobe. O cadastro grava o pendente antes de publicar, então falhar aqui significa que
            // nenhuma mensagem saiu — e pedir o cadastro de novo substitui o pendente e manda outra.
            throw InternalErrorException.with("could not publish the sign-up validation", exception);
        }
    }

    // O token vai na consulta da URL, codificado: ele é base64 de URL e não precisaria, mas
    // depender disso deixaria o link quebrado no dia em que o formato do token mudasse.
    private String confirmationUrlOf(final SignUpValidation aValidation) {
        final var token = URLEncoder.encode(aValidation.token().value(), StandardCharsets.UTF_8);
        final var base = this.properties.confirmationUrl();
        return base.contains("?") ? "%s&token=%s".formatted(base, token) : "%s?token=%s".formatted(base, token);
    }
}
