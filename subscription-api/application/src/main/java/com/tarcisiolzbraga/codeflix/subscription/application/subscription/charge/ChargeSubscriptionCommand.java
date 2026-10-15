package com.tarcisiolzbraga.codeflix.subscription.application.subscription.charge;

// O userId vem do token. O token do cartão vem do corpo, e é só isso que este serviço vê do cartão
// — número e CVV nunca passam por aqui.
public record ChargeSubscriptionCommand(String userId, String creditCardToken) {}
