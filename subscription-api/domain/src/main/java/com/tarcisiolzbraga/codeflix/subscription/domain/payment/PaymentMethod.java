package com.tarcisiolzbraga.codeflix.subscription.domain.payment;

import com.tarcisiolzbraga.codeflix.subscription.domain.ValueObject;

// O meio de pagamento que o provedor guardou, representado por uma referência opaca.
//
// **Nenhum dado de cartão mora aqui**, e é o ponto: número, CVV e validade ficam no provedor, que
// devolve esta referência ao tokenizar. É ela que a cobrança recorrente envia, e ela não serve para
// nada fora do provedor — vazada, não compra nada em lugar nenhum.
//
// Sem bandeira nem últimos quatro dígitos, que um provedor de verdade devolveria para a tela
// mostrar "Visa •••• 4242". O provedor aqui é de mentira e não os devolve, e campo que ninguém
// preenche é o erro do `deletedAt` da referência do curso. Entram quando houver quem os forneça.
public record PaymentMethod(String providerRef) implements ValueObject {

    private static final int MAX_LENGTH = 255;

    public PaymentMethod {
        this.assertArgumentNotEmpty(providerRef, "'providerRef' should not be empty");
        this.assertArgumentMaxLength(
                providerRef, MAX_LENGTH, "'providerRef' should have at most %d characters".formatted(MAX_LENGTH));
    }
}
