package com.tarcisiolzbraga.codeflix.subscription.domain.payment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.tarcisiolzbraga.codeflix.subscription.domain.exceptions.DomainException;
import com.tarcisiolzbraga.codeflix.subscription.domain.money.Money;
import com.tarcisiolzbraga.codeflix.subscription.domain.person.Address;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class PaymentTest {

    private static final String ORDER_ID = "11111111-1111-1111-1111-111111111111";
    private static final String CARD_TOKEN = "tok_abc123";

    private static Money amount() {
        return Money.of("BRL", new BigDecimal("49.90"));
    }

    private static Address address() {
        return new Address("01310930", "1578", "apto 42", "BR");
    }

    @Test
    void givenValidData_whenCreates_thenHoldsIt() {
        final var actual = new Payment(ORDER_ID, amount(), address(), CARD_TOKEN);

        assertEquals(ORDER_ID, actual.orderId());
        assertEquals(amount(), actual.amount());
        assertEquals(CARD_TOKEN, actual.paymentToken());
    }

    @Test
    void givenNoOrderId_whenCreates_thenThrowsDomainException() {
        final var actual = assertThrows(
                DomainException.class, () -> new Payment("  ", amount(), address(), CARD_TOKEN));

        assertEquals("'orderId' should not be empty", actual.getMessage());
    }

    @Test
    void givenNoAmount_whenCreates_thenThrowsDomainException() {
        final var actual = assertThrows(
                DomainException.class, () -> new Payment(ORDER_ID, null, address(), CARD_TOKEN));

        assertEquals("'amount' should not be null", actual.getMessage());
    }

    // Sem endereço de cobrança não se cobra: é dado que o provedor exige para analisar a transação.
    @Test
    void givenNoBillingAddress_whenCreates_thenThrowsDomainException() {
        final var actual = assertThrows(
                DomainException.class, () -> new Payment(ORDER_ID, amount(), null, CARD_TOKEN));

        assertEquals("'billingAddress' should not be null", actual.getMessage());
    }

    @Test
    void givenNoCardToken_whenCreates_thenThrowsDomainException() {
        final var actual =
                assertThrows(DomainException.class, () -> new Payment(ORDER_ID, amount(), address(), null));

        assertEquals("'paymentToken' should not be empty", actual.getMessage());
    }
}
