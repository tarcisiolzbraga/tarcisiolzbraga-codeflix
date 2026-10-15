package com.tarcisiolzbraga.codeflix.subscription.application.account.paymentmethod;

// O token de uso único que o navegador obteve do provedor. Ele é trocado por uma referência durável
// e **não é guardado**: o que fica no banco é a referência.
public record StorePaymentMethodCommand(String userId, String creditCardToken) {}
