package com.tarcisiolzbraga.codeflix.subscription.domain.payment;

import com.tarcisiolzbraga.codeflix.subscription.domain.ValueObject;
import com.tarcisiolzbraga.codeflix.subscription.domain.money.Money;
import com.tarcisiolzbraga.codeflix.subscription.domain.person.Address;

// O que se manda ao provedor para cobrar.
//
// Só cartão, e não a hierarquia selada com Pix que a referência tem. O motivo não é economia: Pix é
// **assíncrono** por natureza — a cobrança gera um código e a confirmação chega depois, quando a
// pessoa paga. Modelá-lo atrás de um processPayment que devolve aprovado ou recusado na hora seria
// mentir sobre como ele funciona, e é o que a referência faz. Quando Pix entrar de verdade, ele
// precisa de um caminho próprio, com confirmação chegando por evento.
//
// O orderId é a chave de idempotência: é por ele que o provedor reconhece a mesma cobrança tentada
// duas vezes e não cobra em dobro.
public record Payment(String orderId, Money amount, Address billingAddress, String creditCardToken)
        implements ValueObject {

    public Payment {
        this.assertArgumentNotEmpty(orderId, "'orderId' should not be empty");
        this.assertArgumentNotNull(amount, "'amount' should not be null");
        this.assertArgumentNotNull(billingAddress, "'billingAddress' should not be null");
        this.assertArgumentNotEmpty(creditCardToken, "'creditCardToken' should not be empty");
    }
}
