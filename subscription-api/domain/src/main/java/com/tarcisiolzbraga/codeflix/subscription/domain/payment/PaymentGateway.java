package com.tarcisiolzbraga.codeflix.subscription.domain.payment;

// Quem cobra. O domínio não sabe qual provedor é, nem fala HTTP.
//
// É síncrono de propósito, e isso é uma afirmação sobre cartão, não sobre pagamento em geral: o
// caso de uso precisa do resultado para decidir o estado da assinatura na mesma passagem. Meio de
// pagamento cuja confirmação chega depois — Pix, boleto — não cabe atrás desta interface, e forçá-lo
// aqui esconderia que a assinatura ficaria num limbo até a confirmação.
public interface PaymentGateway {

    Transaction processPayment(Payment aPayment);
}
