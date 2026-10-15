package com.tarcisiolzbraga.codeflix.subscription.infrastructure.payment;

import com.tarcisiolzbraga.codeflix.subscription.domain.payment.Payment;
import com.tarcisiolzbraga.codeflix.subscription.domain.payment.PaymentGateway;
import com.tarcisiolzbraga.codeflix.subscription.domain.payment.Transaction;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Um provedor de pagamento de mentira, para o ciclo da assinatura rodar inteiro sem provedor de
// verdade. O de verdade entra atrás da mesma interface, sem mexer no domínio nem no caso de uso.
//
// Decide pelo token do cartão, em vez de aprovar sempre como o mock da referência: é o que torna os
// três caminhos do ciclo exercitáveis à mão e no teste ponta a ponta.
//
//   tok_refused…     recusa, com transação aberta — cartão negado
//   tok_unreachable… recusa sem transação — o provedor não respondeu
//   qualquer outro   aprova
//
// Avisa no log a cada cobrança, e isso é de propósito: um provedor falso passando por verdadeiro
// num ambiente de verdade é o tipo de coisa que ninguém descobre até a fatura não chegar.
public class FakePaymentGateway implements PaymentGateway {

    private static final Logger LOG = LoggerFactory.getLogger(FakePaymentGateway.class);

    private static final String REFUSED_PREFIX = "tok_refused";
    private static final String UNREACHABLE_PREFIX = "tok_unreachable";

    @Override
    public Transaction processPayment(final Payment aPayment) {
        LOG.warn(
                "cobrança processada por provedor DE MENTIRA [pedido:{}] [valor:{} {}]",
                aPayment.orderId(),
                aPayment.amount().currencyCode(),
                aPayment.amount().amount());

        final var token = aPayment.creditCardToken();
        if (token.startsWith(UNREACHABLE_PREFIX)) {
            return new Transaction.Failed(null, "the payment provider could not be reached");
        }
        if (token.startsWith(REFUSED_PREFIX)) {
            return new Transaction.Failed(transactionId(), "the card was refused");
        }
        return new Transaction.Succeeded(transactionId());
    }

    private static String transactionId() {
        return "fake-" + UUID.randomUUID();
    }
}
