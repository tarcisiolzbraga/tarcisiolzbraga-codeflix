package com.tarcisiolzbraga.codeflix.subscription.application.subscription.subscribe;

// O userId vem do token, não do corpo: quem assina, assina para si.
public record SubscribeCommand(String userId, String planId) {}
