package com.tarcisiolzbraga.codeflix.subscription.infrastructure.account.models;

// O token de uso único que o navegador obteve do provedor, e nada mais do cartão: número e CVV não
// têm campo onde entrar. Este serviço troca o token por uma referência durável e guarda só ela.
public record PaymentMethodRequest(String creditCardToken) {}
