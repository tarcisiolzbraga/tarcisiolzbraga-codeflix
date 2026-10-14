package com.tarcisiolzbraga.codeflix.subscription.infrastructure.account.models;

// O token vem no corpo, não na URL: em query string ele acabaria no log do servidor e no histórico
// do navegador, e ele é credencial.
public record ConfirmSignUpRequest(String token) {}
