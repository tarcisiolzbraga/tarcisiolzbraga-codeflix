package com.tarcisiolzbraga.codeflix.subscription.e2e.subscription;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tarcisiolzbraga.codeflix.subscription.infrastructure.E2ETest;
import com.tarcisiolzbraga.codeflix.subscription.infrastructure.KeycloakTestToken;
import com.tarcisiolzbraga.codeflix.subscription.infrastructure.KeycloakTestUserToken;
import com.tarcisiolzbraga.codeflix.subscription.infrastructure.AdvanceableClock;
import com.tarcisiolzbraga.codeflix.subscription.infrastructure.messaging.AmqpProperties;
import com.tarcisiolzbraga.codeflix.subscription.infrastructure.messaging.models.SignUpValidationMessage;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

// A jornada de quem assina, sobre a API de verdade: Tomcat, MySQL, Keycloak e RabbitMQ em container.
//
// A pessoa nasce pelo próprio cadastro — e é por isso que esta jornada existe: ela prova que o
// caminho inteiro fecha, do cadastro ao cancelamento, com um token de pessoa de verdade emitido pelo
// Keycloak. Nada é semeado no banco por baixo, e a role de conta é a que a confirmação concedeu.
//
// Não repete asserção de contrato: status e corpo ficam no SubscriptionControllerTest e nos testes
// de JSON, e o comportamento do SQL no SubscriptionJdbcGatewayIT.
@E2ETest
class SubscriptionE2ETest implements SubscriptionE2EDsl {

    private static final String PASSWORD = "uma-senha-boa";
    private static final String CPF = "11144477735";
    private static final String GOOD_CARD = "tok_visa_4242";
    private static final String REFUSED_CARD = "tok_refused_card";
    private static final Duration A_MONTH_AND_A_DAY = Duration.ofDays(32);
    private static final Duration PAST_THE_TOLERANCE = Duration.ofDays(3);

    @LocalServerPort
    private int port;

    @Autowired
    private KeycloakTestToken operatorToken;

    @Autowired
    private KeycloakTestUserToken userToken;

    // O relógio que a jornada adianta; vem do JourneyClockConfiguration, pelo @E2ETest.
    @Autowired
    private AdvanceableClock clock;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AmqpProperties amqpProperties;

    @Override
    public int port() {
        return this.port;
    }

    @BeforeEach
    void startFromTheRealTime() {
        this.clock.reset();
    }

    @Test
    void givenAPersonWhoJustSignedUp_whenSubscribesToAPlanOnSale_thenItStartsAsATrial() {
        final var plan = publishPlan(operator(), "Plus", "49.90");
        final var subscriber = aConfirmedSubscriber();

        final var response = subscribeTo(subscriber, plan);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals("/subscriptions/me", response.getHeaders().getFirst("Location"));
        final var body = response.getBody();
        assertNotNull(body);
        assertEquals("trailing", body.status());
        assertEquals(plan, body.planId());
        assertNull(body.lastRenewDate());
        assertEquals(mySubscription(subscriber).id(), body.id());
    }

    // O cadastro concede a role de **conta**, e sem ela esta jornada toda responderia 403. A role do
    // catálogo é outra, e não vem daqui: vem com a assinatura.
    @Test
    void givenAPersonWhoNeverSubscribed_whenAsksForItsSubscription_thenAnswersNotFoundAndNotForbidden() {
        final var subscriber = aConfirmedSubscriber();

        final var actual = statusOfMySubscription(subscriber);

        assertEquals(HttpStatus.NOT_FOUND, actual);
    }

    @Test
    void givenAnActiveSubscription_whenSubscribesAgain_thenAnswersConflict() {
        final var plan = publishPlan(operator(), "Plus", "49.90");
        final var subscriber = aConfirmedSubscriber();
        subscribeTo(subscriber, plan);

        final var actual = subscribeTo(subscriber, plan);

        assertEquals(HttpStatus.CONFLICT, actual.getStatusCode());
    }

    // Antes do vencimento não se cobra: o provedor nem é chamado, e nada muda na assinatura.
    @Test
    void givenASubscriptionNotDueYet_whenCharges_thenNothingIsChargedAndNothingChanges() {
        final var plan = publishPlan(operator(), "Plus", "49.90");
        final var subscriber = aConfirmedSubscriber();
        setBillingAddress(subscriber);
        subscribeTo(subscriber, plan);

        final var actual = chargeWith(subscriber, GOOD_CARD);

        assertFalse(actual.charged());
        assertNull(actual.transactionId());
        assertEquals("trailing", actual.subscription().status());
    }

    @Test
    void givenADueSubscriptionAndAGoodCard_whenCharges_thenItBecomesActiveAndTheDueDateMoves() {
        final var plan = publishPlan(operator(), "Plus", "49.90");
        final var subscriber = aConfirmedSubscriber();
        setBillingAddress(subscriber);
        final var dueDateWhenItStarted = mySubscriptionAfterSubscribing(subscriber, plan);

        this.clock.advanceBy(A_MONTH_AND_A_DAY);
        final var actual = chargeWith(subscriber, GOOD_CARD);

        assertTrue(actual.charged());
        assertNotNull(actual.transactionId());
        assertEquals("active", actual.subscription().status());
        assertNotEquals(dueDateWhenItStarted, actual.subscription().dueDate());
        assertNotNull(actual.subscription().lastRenewDate());
        assertEquals("active", mySubscription(subscriber).status());
    }

    // Cartão recusado dentro da tolerância: a assinatura fica incompleta, viva, esperando outro
    // cartão — e a resposta ainda é 200, porque a requisição foi atendida.
    @Test
    void givenADueSubscriptionAndARefusedCard_whenCharges_thenItBecomesIncomplete() {
        final var plan = publishPlan(operator(), "Plus", "49.90");
        final var subscriber = aConfirmedSubscriber();
        setBillingAddress(subscriber);
        subscribeTo(subscriber, plan);

        this.clock.advanceBy(A_MONTH_AND_A_DAY);
        final var actual = chargeWith(subscriber, REFUSED_CARD);

        assertFalse(actual.charged());
        assertEquals("the card was refused", actual.reason());
        assertEquals("incomplete", actual.subscription().status());
    }

    // Quem resolve o cartão volta a ficar em dia: é o caminho que justifica a rota existir em vez de
    // só o job que varre vencidas.
    @Test
    void givenAnIncompleteSubscription_whenChargesWithAnotherCard_thenItBecomesActiveAgain() {
        final var plan = publishPlan(operator(), "Plus", "49.90");
        final var subscriber = aConfirmedSubscriber();
        setBillingAddress(subscriber);
        subscribeTo(subscriber, plan);
        this.clock.advanceBy(A_MONTH_AND_A_DAY);
        chargeWith(subscriber, REFUSED_CARD);

        final var actual = chargeWith(subscriber, GOOD_CARD);

        assertTrue(actual.charged());
        assertEquals("active", actual.subscription().status());
    }

    // Passada a tolerância, a recusa cancela: é o que impede uma assinatura nunca paga de ficar viva
    // para sempre.
    @Test
    void givenASubscriptionRefusedPastTheTolerance_whenChargesAgain_thenItIsCanceled() {
        final var plan = publishPlan(operator(), "Plus", "49.90");
        final var subscriber = aConfirmedSubscriber();
        setBillingAddress(subscriber);
        subscribeTo(subscriber, plan);
        this.clock.advanceBy(A_MONTH_AND_A_DAY);
        chargeWith(subscriber, REFUSED_CARD);

        this.clock.advanceBy(PAST_THE_TOLERANCE);
        final var actual = chargeWith(subscriber, REFUSED_CARD);

        assertFalse(actual.charged());
        assertEquals("canceled", actual.subscription().status());
    }

    @Test
    void givenAnActiveSubscription_whenCancelsTwice_thenItStaysCanceledWithoutError() {
        final var plan = publishPlan(operator(), "Plus", "49.90");
        final var subscriber = aConfirmedSubscriber();
        subscribeTo(subscriber, plan);

        final var first = cancelMySubscription(subscriber);
        final var second = cancelMySubscription(subscriber);

        assertEquals("canceled", first.status());
        assertEquals("canceled", second.status());
        assertEquals(first.id(), second.id());
    }

    // Cancelada não tranca a conta: reassinar é legítimo, e a assinatura nova é outra.
    @Test
    void givenACanceledSubscription_whenSubscribesAgain_thenANewSubscriptionIsCreated() {
        final var plan = publishPlan(operator(), "Plus", "49.90");
        final var subscriber = aConfirmedSubscriber();
        final var first = subscribeTo(subscriber, plan).getBody();
        assertNotNull(first);
        cancelMySubscription(subscriber);

        final var actual = subscribeTo(subscriber, plan);

        assertEquals(HttpStatus.CREATED, actual.getStatusCode());
        final var second = actual.getBody();
        assertNotNull(second);
        assertNotEquals(first.id(), second.id());
        assertEquals("trailing", second.status());
    }

    // A promessa do `/me`: não há caminho para pedir a assinatura de outro, e o token de cada um só
    // alcança a própria. Com id no caminho isto dependeria de uma conferência em cada rota.
    @Test
    void givenTwoSubscribers_whenEachAsksForItsOwn_thenNeitherSeesTheOther() {
        final var plan = publishPlan(operator(), "Plus", "49.90");
        final var mine = aConfirmedSubscriber();
        final var theirs = aConfirmedSubscriber();
        subscribeTo(mine, plan);
        subscribeTo(theirs, plan);

        assertNotEquals(mySubscription(mine).id(), mySubscription(theirs).id());
    }

    @Test
    void givenAPlanOutOfSale_whenSubscribes_thenAnswersUnprocessable() {
        final var plan = publishPlan(operator(), "Fora de venda", "9.90");
        client(operator()).put().uri("/plans/{id}/deactivate", plan).retrieve().toBodilessEntity();
        final var subscriber = aConfirmedSubscriber();

        final var actual = subscribeTo(subscriber, plan);

        assertEquals(HttpStatus.UNPROCESSABLE_CONTENT, actual.getStatusCode());
    }

    @Test
    void givenAnAccountWithoutBillingAddress_whenCharges_thenAnswersUnprocessable() {
        final var plan = publishPlan(operator(), "Plus", "49.90");
        final var subscriber = aConfirmedSubscriber();
        subscribeTo(subscriber, plan);
        this.clock.advanceBy(A_MONTH_AND_A_DAY);

        final var actual = client(subscriber)
                .post()
                .uri("/subscriptions/me/charge")
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"creditCardToken\":\"%s\"}".formatted(GOOD_CARD))
                .retrieve()
                .toBodilessEntity();

        assertEquals(HttpStatus.UNPROCESSABLE_CONTENT, actual.getStatusCode());
    }

    private String operator() {
        return this.operatorToken.bearer();
    }

    // Uma pessoa nova, pelo caminho que uma pessoa segue: cadastra, pega o token no e-mail que foi
    // para a fila, confirma e entra com e-mail e senha. O bearer que sai daqui tem o `sub` dela.
    private String aConfirmedSubscriber() {
        final var email = "assinante-%s@codeflix.com.br".formatted(UUID.randomUUID());
        final var body = """
                {"firstname":"Tarcisio","lastname":"Braga","email":"%s","password":"%s",\
                "documentNumber":"%s","documentType":"cpf"}"""
                .formatted(email, PASSWORD, CPF);
        RestClient.create()
                .post()
                .uri("http://localhost:%d/accounts/sign-up".formatted(this.port))
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toBodilessEntity();

        confirm(tokenFromTheEmailOf(email));
        return this.userToken.bearerFor(email, PASSWORD);
    }

    private void confirm(final String token) {
        RestClient.create()
                .post()
                .uri("http://localhost:%d/accounts/sign-up/confirm".formatted(this.port))
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"token\":\"%s\"}".formatted(token))
                .retrieve()
                .toBodilessEntity();
    }

    // Procura na fila o e-mail desta pessoa: a jornada cria mais de uma, e pegar a primeira
    // mensagem daria o token de outra.
    private String tokenFromTheEmailOf(final String email) {
        final var queue = this.amqpProperties.queues().signUpValidation().queue();
        for (var attempt = 0; attempt < 20; attempt++) {
            final var message = this.rabbitTemplate.receive(queue, 1_000L);
            if (message == null) {
                continue;
            }
            final var payload =
                    this.objectMapper.readValue(new String(message.getBody()), SignUpValidationMessage.class);
            if (email.equals(payload.to())) {
                final var url = payload.confirmationUrl();
                return url.substring(url.indexOf("token=") + "token=".length());
            }
        }
        throw new AssertionError("nenhum e-mail de validação para %s chegou na fila %s".formatted(email, queue));
    }

    private String mySubscriptionAfterSubscribing(final String subscriber, final String plan) {
        final var created = subscribeTo(subscriber, plan).getBody();
        assertNotNull(created);
        return created.dueDate().toString();
    }
}
