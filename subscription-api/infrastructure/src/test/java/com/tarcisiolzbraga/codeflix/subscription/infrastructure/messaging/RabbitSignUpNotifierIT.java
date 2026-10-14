package com.tarcisiolzbraga.codeflix.subscription.infrastructure.messaging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tarcisiolzbraga.codeflix.subscription.domain.account.AccountFixture;
import com.tarcisiolzbraga.codeflix.subscription.domain.account.signup.SignUpNotifier;
import com.tarcisiolzbraga.codeflix.subscription.domain.account.signup.SignUpToken;
import com.tarcisiolzbraga.codeflix.subscription.domain.account.signup.SignUpValidation;
import com.tarcisiolzbraga.codeflix.subscription.infrastructure.IntegrationTest;
import com.tarcisiolzbraga.codeflix.subscription.infrastructure.messaging.models.SignUpValidationMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.ObjectMapper;

// Contra um RabbitMQ de verdade, que sobe vazio: a topologia é declarada pelo AmqpConfig na subida
// da aplicação, e se ela estivesse errada a mensagem não chegaria a fila alguma.
@IntegrationTest
class RabbitSignUpNotifierIT {

    private static final long TIMEOUT = 5_000L;

    @Autowired
    private SignUpNotifier notifier;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private AmqpProperties properties;

    @Autowired
    private ObjectMapper mapper;

    // Esvazia a fila: o broker é um só para a execução toda, e mensagem de um teste anterior faria o
    // seguinte afirmar sobre o que não publicou.
    @BeforeEach
    void drainTheQueue() {
        while (this.rabbitTemplate.receive(queue(), 100L) != null) {
            // descarta
        }
    }

    @Test
    void givenAValidation_whenSends_thenItLandsOnTheQueue() {
        final var validation = validationWith(SignUpToken.generate());

        notifier.sendValidation(validation);

        assertNotNull(received());
    }

    @Test
    void givenAValidation_whenSends_thenTheMessageSaysWhoToMailAndHowToGreet() {
        final var validation = validationWith(SignUpToken.generate());

        notifier.sendValidation(validation);

        final var actual = received();
        assertEquals(AccountFixture.EMAIL, actual.to());
        assertEquals(AccountFixture.FIRSTNAME, actual.firstname());
    }

    // O link é montado deste lado: o serviço de e-mail recebe pronto e não conhece rota nossa.
    @Test
    void givenAValidation_whenSends_thenTheLinkCarriesTheTokenOnTheConfiguredPage() {
        final var token = SignUpToken.generate();

        notifier.sendValidation(validationWith(token));

        final var actual = received().confirmationUrl();
        assertEquals("https://codeflix.test/cadastro/confirmar?token=" + token.value(), actual);
    }

    // O resumo é o que fica no banco; o que vai no e-mail é o token em claro, senão o link não
    // confirmaria nada.
    @Test
    void givenAValidation_whenSends_thenTheLinkCarriesTheTokenAndNotItsDigest() {
        final var token = SignUpToken.generate();

        notifier.sendValidation(validationWith(token));

        final var actual = received().confirmationUrl();
        assertTrue(actual.contains(token.value()));
        assertTrue(!actual.contains(token.hashed()));
    }

    @Test
    void givenAValidation_whenSends_thenTheMessageIsJson() {
        notifier.sendValidation(validationWith(SignUpToken.generate()));

        final var message = this.rabbitTemplate.receive(queue(), TIMEOUT);

        assertNotNull(message);
        assertEquals("application/json", message.getMessageProperties().getContentType());
    }

    private SignUpValidation validationWith(final SignUpToken token) {
        return new SignUpValidation(AccountFixture.email(), AccountFixture.name(), token);
    }

    private SignUpValidationMessage received() {
        final var message = this.rabbitTemplate.receive(queue(), TIMEOUT);
        assertNotNull(message, "nada chegou na fila " + queue());
        return this.mapper.readValue(new String(message.getBody()), SignUpValidationMessage.class);
    }

    private String queue() {
        return this.properties.queues().signUpValidation().queue();
    }
}
