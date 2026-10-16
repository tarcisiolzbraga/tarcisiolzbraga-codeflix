package com.tarcisiolzbraga.codeflix.subscription.e2e.account;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tarcisiolzbraga.codeflix.subscription.domain.account.idp.IdentityProviderGateway;
import com.tarcisiolzbraga.codeflix.subscription.domain.account.signup.SignUpToken;
import com.tarcisiolzbraga.codeflix.subscription.domain.person.Email;
import com.tarcisiolzbraga.codeflix.subscription.infrastructure.E2ETest;
import com.tarcisiolzbraga.codeflix.subscription.infrastructure.messaging.AmqpProperties;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import tools.jackson.databind.ObjectMapper;

// A jornada de quem se cadastra, sobre a API de verdade: Tomcat, MySQL, Keycloak e RabbitMQ em
// container. O token vem da fila, que é onde o e-mail estaria — nada é semeado no banco por baixo.
@E2ETest
class SignUpE2ETest implements SignUpE2EDsl {

    private static final String CPF = "11144477735";

    @LocalServerPort
    private int port;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AmqpProperties amqpProperties;

    @Autowired
    private IdentityProviderGateway identityProvider;

    @Override
    public int port() {
        return this.port;
    }

    @Override
    public RabbitTemplate rabbit() {
        return this.rabbitTemplate;
    }

    @Override
    public ObjectMapper mapper() {
        return this.objectMapper;
    }

    @Override
    public String validationQueue() {
        return this.amqpProperties.queues().signUpValidation().queue();
    }

    @BeforeEach
    void startFromAnEmptyQueue() {
        drainTheQueue();
    }

    @Test
    void givenSomeoneNew_whenSignsUpAndConfirmsWithTheEmailedToken_thenTheAccountIsCreated() {
        final var email = anEmail();

        assertEquals(HttpStatus.ACCEPTED, signUp(email, CPF));

        final var actual = confirm(tokenFromTheEmail());
        assertEquals(HttpStatus.OK, actual.status());
        assertNotNull(actual.accountId());
    }

    // Até confirmar, a pessoa existe no provedor com o e-mail não conferido; depois, conferido.
    @Test
    void givenSomeoneNew_whenSignsUp_thenTheProviderHasThemWithTheEmailUnverified() {
        final var email = anEmail();

        signUp(email, CPF);

        final var actual = this.identityProvider.userOfEmail(new Email(email)).orElseThrow();
        assertFalse(actual.emailVerified());
    }

    @Test
    void givenSomeoneWhoConfirmed_whenLooksAtTheProvider_thenTheEmailIsVerified() {
        final var email = anEmail();
        signUp(email, CPF);

        confirm(tokenFromTheEmail());

        assertTrue(this.identityProvider.userOfEmail(new Email(email)).orElseThrow().emailVerified());
    }

    // Clicar no link duas vezes devolve a mesma conta, e não um erro.
    @Test
    void givenAConfirmedSignUp_whenConfirmsAgainWithTheSameToken_thenAnswersTheSameAccount() {
        final var email = anEmail();
        signUp(email, CPF);
        final var token = tokenFromTheEmail();
        final var first = confirm(token);

        final var actual = confirm(token);

        assertEquals(HttpStatus.OK, actual.status());
        assertEquals(first.accountId(), actual.accountId());
    }

    // O caso que a validação por e-mail existe para cobrir: o e-mail já existe no provedor e não
    // tem conta. Pedir de novo reaproveita a pessoa de lá e manda um link novo — e só o novo vale.
    @Test
    void givenAnUnconfirmedSignUp_whenSignsUpAgain_thenOnlyTheNewestLinkWorks() {
        final var email = anEmail();
        signUp(email, CPF);
        final var oldToken = tokenFromTheEmail();

        signUp(email, CPF);
        final var newToken = tokenFromTheEmail();

        assertEquals(HttpStatus.NOT_FOUND, confirm(oldToken).status());
        assertEquals(HttpStatus.OK, confirm(newToken).status());
    }

    // Quem já tem conta recebe a mesma resposta e nenhum e-mail: a resposta não conta a um estranho
    // se o endereço está cadastrado.
    @Test
    void givenSomeoneWhoAlreadyHasAnAccount_whenSignsUpAgain_thenTheAnswerIsTheSameAndNoEmailIsSent() {
        final var email = anEmail();
        signUp(email, CPF);
        confirm(tokenFromTheEmail());

        final var actual = signUp(email, CPF);

        assertEquals(HttpStatus.ACCEPTED, actual);
        assertTrue(noEmailWasSent());
    }

    @Test
    void givenATokenThatNeverExisted_whenConfirms_thenAnswersNotFound() {
        final var actual = confirm(SignUpToken.generate().value());

        assertEquals(HttpStatus.NOT_FOUND, actual.status());
    }

    @Test
    void givenAnInvalidDocument_whenSignsUp_thenAnswersUnprocessableAndSendsNothing() {
        final var actual = signUp(anEmail(), "11111111111");

        assertEquals(HttpStatus.UNPROCESSABLE_CONTENT, actual);
        assertTrue(noEmailWasSent());
    }

    // E-mail diferente por teste: o realm do Keycloak não é esvaziado entre eles.
    private static String anEmail() {
        return "assinante-%s@codeflix.com.br".formatted(UUID.randomUUID());
    }
}
