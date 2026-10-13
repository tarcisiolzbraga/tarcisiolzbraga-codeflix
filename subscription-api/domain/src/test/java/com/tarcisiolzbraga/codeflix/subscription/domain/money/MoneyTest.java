package com.tarcisiolzbraga.codeflix.subscription.domain.money;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.tarcisiolzbraga.codeflix.subscription.domain.exceptions.DomainException;
import java.math.BigDecimal;
import java.util.Currency;
import org.junit.jupiter.api.Test;

class MoneyTest {

    private static final String BRL = "BRL";

    @Test
    void givenCurrencyAndAmount_whenCreates_thenHoldsBoth() {
        final var amount = new BigDecimal("49.90");

        final var actual = Money.of(BRL, amount);

        assertEquals(Currency.getInstance(BRL), actual.currency());
    }

    @Test
    void givenAmountWithFewerDecimals_whenCreates_thenNormalizesToTwo() {
        final var amount = new BigDecimal("50");

        final var actual = Money.of(BRL, amount);

        assertEquals(new BigDecimal("50.00"), actual.amount());
    }

    @Test
    void givenAmountWithMoreDecimals_whenCreates_thenRoundsHalfUp() {
        final var amount = new BigDecimal("49.905");

        final var actual = Money.of(BRL, amount);

        assertEquals(new BigDecimal("49.91"), actual.amount());
    }

    // O ponto de guardar BigDecimal normalizado: 50 e 50.00 são o mesmo dinheiro, e value object é
    // igual pelos valores. Sem normalizar a escala, o equals do BigDecimal diria que não são.
    @Test
    void givenTheSameAmountWrittenDifferently_whenComparesThem_thenTheyAreEqual() {
        final var fifty = Money.of(BRL, new BigDecimal("50"));

        final var actual = Money.of(BRL, new BigDecimal("50.00"));

        assertEquals(fifty, actual);
    }

    @Test
    void givenZeroAmount_whenCreates_thenItIsAllowedBecauseAFreePlanIsAPlan() {
        final var amount = BigDecimal.ZERO;

        final var actual = Money.of(BRL, amount);

        assertEquals(new BigDecimal("0.00"), actual.amount());
    }

    @Test
    void givenNegativeAmount_whenCreates_thenThrowsDomainException() {
        final var amount = new BigDecimal("-0.01");

        final var actual = assertThrows(DomainException.class, () -> Money.of(BRL, amount));

        assertEquals("'amount' should not be negative", actual.getMessage());
    }

    @Test
    void givenNullAmount_whenCreates_thenThrowsDomainException() {
        final BigDecimal amount = null;

        final var actual = assertThrows(DomainException.class, () -> Money.of(BRL, amount));

        assertEquals("'amount' should not be null", actual.getMessage());
    }

    @Test
    void givenNullCurrency_whenCreates_thenThrowsDomainException() {
        final String currency = null;

        final var actual = assertThrows(DomainException.class, () -> Money.of(currency, BigDecimal.ONE));

        assertEquals("'currency' should not be null", actual.getMessage());
    }

    // O Currency.getInstance lança IllegalArgumentException, que chegaria ao Spring como 500: isto é
    // entrada inválida e tem de virar DomainException, como qualquer outra recusa do domínio.
    @Test
    void givenUnknownCurrencyCode_whenCreates_thenThrowsDomainExceptionAndNotFrameworkError() {
        final var currency = "XYZ";

        final var actual = assertThrows(DomainException.class, () -> Money.of(currency, BigDecimal.ONE));

        assertEquals("'XYZ' is not a valid currency", actual.getMessage());
    }
}
