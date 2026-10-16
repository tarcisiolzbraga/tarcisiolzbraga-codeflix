package com.tarcisiolzbraga.codeflix.subscription.domain.payment;

// Quem cobra. O domínio não sabe qual provedor é, nem fala HTTP.
//
// É síncrono de propósito, e isso é uma afirmação sobre cartão, não sobre pagamento em geral: o
// caso de uso precisa do resultado para decidir o estado da assinatura na mesma passagem. Meio de
// pagamento cuja confirmação chega depois — Pix, boleto — não cabe atrás desta interface, e forçá-lo
// aqui esconderia que a assinatura ficaria num limbo até a confirmação.
public interface PaymentGateway {

    Transaction processPayment(Payment aPayment);

    // Troca um token de uso único, que o navegador obteve do provedor, por uma referência durável
    // que o provedor guarda. É ela que a cobrança automática envia depois, e é o que permite cobrar
    // sem a pessoa presente com o cartão na mão.
    //
    // O dado do cartão não passa por aqui nem numa direção: entra um token, sai uma referência.
    PaymentMethod storeCard(String aCreditCardToken);
}
