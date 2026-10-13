package com.tarcisiolzbraga.codeflix.subscription.domain.money;

import com.tarcisiolzbraga.codeflix.subscription.domain.ValueObject;
import com.tarcisiolzbraga.codeflix.subscription.domain.exceptions.DomainException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;

// BigDecimal, e não Double como na referência: a coluna é DECIMAL(15,2) e dinheiro em ponto
// flutuante acumula erro em soma e comparação. Isto é correção, não preferência.
//
// A escala é normalizada para 2 na construção, e isso é o que faz a igualdade funcionar: 50 e 50.00
// são o mesmo dinheiro, mas o equals do BigDecimal olha a escala e diria que não são — e value
// object é igual pelos valores.
public record Money(Currency currency, BigDecimal amount) implements ValueObject {

    private static final int SCALE = 2;

    public Money {
        this.assertArgumentNotNull(currency, "'currency' should not be null");
        this.assertArgumentNotNull(amount, "'amount' should not be null");
        this.assertConditionTrue(amount.signum() >= 0, "'amount' should not be negative");
        amount = amount.setScale(SCALE, RoundingMode.HALF_UP);
    }

    // O caminho de entrada quando a moeda vem como texto — payload, coluna, configuração. O
    // getInstance recusa código desconhecido com IllegalArgumentException, que chegaria ao Spring
    // como 500: aqui ela vira DomainException, como qualquer outra entrada inválida.
    public static Money of(final String currencyCode, final BigDecimal amount) {
        return new Money(currencyOf(currencyCode), amount);
    }

    public String currencyCode() {
        return this.currency.getCurrencyCode();
    }

    private static Currency currencyOf(final String currencyCode) {
        if (currencyCode == null) {
            throw DomainException.with("'currency' should not be null");
        }
        try {
            return Currency.getInstance(currencyCode);
        } catch (final IllegalArgumentException exception) {
            throw DomainException.with("'%s' is not a valid currency".formatted(currencyCode));
        }
    }
}
