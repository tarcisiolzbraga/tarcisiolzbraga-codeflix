package com.tarcisiolzbraga.codeflix.subscription.infrastructure.subscription.models;

// Só o plano: quem assina vem do token.
public record SubscribeRequest(String planId) {}
