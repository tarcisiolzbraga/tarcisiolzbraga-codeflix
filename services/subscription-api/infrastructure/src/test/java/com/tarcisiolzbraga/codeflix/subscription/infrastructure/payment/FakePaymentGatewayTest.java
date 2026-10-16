package com.tarcisiolzbraga.codeflix.subscription.infrastructure.payment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tarcisiolzbraga.codeflix.subscription.domain.money.Money;
import com.tarcisiolzbraga.codeflix.subscription.domain.payment.Payment;
import com.tarcisiolzbraga.codeflix.subscription.domain.payment.Transaction;
import com.tarcisiolzbraga.codeflix.subscription.domain.person.Address;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

// Testa um provedor de mentira, e isso tem valor: o teste ponta a ponta depende destes três
// caminhos para exercitar o ciclo, e um dia o provedor de verdade entra no lugar dele. Se o
// contrato do que ele devolve mudar sem ninguém notar, é aqui que fica vermelho.
class FakePaymentGatewayTest {

    private final FakePaymentGateway gateway = new FakePaymentGateway();

    @Test
    void givenAnyCardToken_whenProcesses_thenApprovesWithATransactionId() {
        final var payment = paymentWith("tok_visa_4242");

        final var actual = gateway.processPayment(payment);

        final var succeeded = assertInstanceOf(Transaction.Succeeded.class, actual);
        assertNotNull(succeeded.transactionId());
    }

    // O prefixo do token é o que escolhe o caminho; o valor da cobrança não entra na decisão.
    @Test
    void givenTheRefusedToken_whenProcesses_thenFailsWithATransactionIdAndAReason() {
        final var payment = paymentWith("tok_refused_card");

        final var actual = gateway.processPayment(payment);

        final var failed = assertInstanceOf(Transaction.Failed.class, actual);
        assertNotNull(failed.transactionId());
        assertEquals("the card was refused", failed.reason());
    }

    // Provedor fora do ar não abre transação, e é por isso que o id vem nulo: não há transação a
    // que se referir. É o caso que fez o MarkIncomplete aceitar id de transação opcional.
    @Test
    void givenTheUnreachableToken_whenProcesses_thenFailsWithoutATransactionId() {
        final var payment = paymentWith("tok_unreachable_provider");

        final var actual = gateway.processPayment(payment);

        final var failed = assertInstanceOf(Transaction.Failed.class, actual);
        assertNull(failed.transactionId());
        assertEquals("the payment provider could not be reached", failed.reason());
    }

    @Test
    void givenTwoPayments_whenProcesses_thenEachOneGetsItsOwnTransactionId() {
        final var first = assertInstanceOf(
                Transaction.Succeeded.class, gateway.processPayment(paymentWith("tok_visa_4242")));
        final var second = assertInstanceOf(
                Transaction.Succeeded.class, gateway.processPayment(paymentWith("tok_visa_4242")));

        assertTrue(!first.transactionId().equals(second.transactionId()));
    }

    // A referência guardada carrega o prefixo do token original, e isso não é enfeite: é o que
    // mantém os três caminhos exercitáveis quando a cobrança automática usa o meio guardado — um
    // cartão recusado, guardado, segue recusando.
    @Test
    void givenACardToken_whenStoresIt_thenTheReferenceKeepsThePrefixSoTheOutcomeSurvives() {
        final var stored = gateway.storeCard("tok_refused_card");

        final var actual = gateway.processPayment(paymentWith(stored.providerRef()));

        final var failed = assertInstanceOf(Transaction.Failed.class, actual);
        assertEquals("the card was refused", failed.reason());
    }

    @Test
    void givenAGoodCardToken_whenStoredAndCharged_thenItIsApproved() {
        final var stored = gateway.storeCard("tok_visa_4242");

        final var actual = gateway.processPayment(paymentWith(stored.providerRef()));

        assertInstanceOf(Transaction.Succeeded.class, actual);
    }

    @Test
    void givenACardToken_whenStoresIt_thenTheReferenceIsNotTheTokenItself() {
        final var actual = gateway.storeCard("tok_visa_4242");

        assertTrue(
                !"tok_visa_4242".equals(actual.providerRef()),
                "a referência guardada não deveria ser o token de uso único");
    }

    private static Payment paymentWith(final String creditCardToken) {
        return new Payment(
                "11111111-1111-1111-1111-111111111111",
                Money.of("BRL", new BigDecimal("50.00")),
                new Address("01310930", "1578", "apto 42", "BR"),
                creditCardToken);
    }
}
