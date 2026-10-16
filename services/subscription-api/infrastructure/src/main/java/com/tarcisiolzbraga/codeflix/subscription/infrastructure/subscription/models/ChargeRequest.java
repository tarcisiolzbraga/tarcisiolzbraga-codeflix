package com.tarcisiolzbraga.codeflix.subscription.infrastructure.subscription.models;

// O token do cartão, e nada mais do cartão: número e CVV nunca passam por este serviço — quem os
// troca por um token é o provedor, direto com o navegador de quem assina.
public record ChargeRequest(String creditCardToken) {}
