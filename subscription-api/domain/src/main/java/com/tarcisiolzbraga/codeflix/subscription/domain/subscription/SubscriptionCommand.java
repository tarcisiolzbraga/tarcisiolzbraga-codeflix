package com.tarcisiolzbraga.codeflix.subscription.domain.subscription;

import com.tarcisiolzbraga.codeflix.subscription.domain.AssertionConcern;

// Um comando por regra.
//
// Sem o ChangeStatus da referência: lá ele existe para o estado conseguir pedir ao agregado que
// troque de estado, por causa da referência de volta. Com a transição devolvendo o próximo estado,
// esse comando perdeu a razão de existir.
public sealed interface SubscriptionCommand extends AssertionConcern {

    // A cobrança passou: ativa, empurra o vencimento e registra a transação.
    record Renew(String transactionId) implements SubscriptionCommand {
        public Renew {
            this.assertArgumentNotEmpty(transactionId, "'transactionId' should not be empty");
        }
    }

    // A cobrança falhou e ainda há prazo. O vencimento NÃO anda: é o que mantém a assinatura
    // vencida e faz a próxima tentativa acontecer.
    //
    // A transação é opcional aqui, ao contrário do Renew: o provedor recusa antes de abrir
    // transação quando a comunicação falha, e nesse caso não há id. Exigir um obrigaria quem chama
    // a inventar um valor, e id inventado no banco é pior que coluna nula.
    record MarkIncomplete(String reason, String transactionId) implements SubscriptionCommand {
        public MarkIncomplete {
            this.assertArgumentNotEmpty(reason, "'reason' should not be empty");
        }
    }

    record Cancel() implements SubscriptionCommand {}
}
