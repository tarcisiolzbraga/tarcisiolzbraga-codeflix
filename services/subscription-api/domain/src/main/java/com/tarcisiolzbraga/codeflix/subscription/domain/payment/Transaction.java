package com.tarcisiolzbraga.codeflix.subscription.domain.payment;

import com.tarcisiolzbraga.codeflix.subscription.domain.ValueObject;

// O resultado da cobrança: um ou outro, e o tipo garante isso.
//
// Na referência é um record só, com um booleano de sucesso e um motivo que pode ser nulo — e aí
// "aprovado com motivo de recusa" é construível, e todo leitor precisa confiar que ninguém montou
// essa combinação. Aqui aprovado não tem onde guardar motivo, e recusado sempre tem um.
public sealed interface Transaction extends ValueObject {

    record Succeeded(String transactionId) implements Transaction {
        public Succeeded {
            this.assertArgumentNotEmpty(transactionId, "'transactionId' should not be empty");
        }
    }

    // O id pode faltar: o provedor recusa antes de abrir transação quando a comunicação falha, e
    // nesse caso não há o que registrar além do motivo.
    record Failed(String transactionId, String reason) implements Transaction {
        public Failed {
            this.assertArgumentNotEmpty(reason, "'reason' should not be empty");
        }
    }
}
