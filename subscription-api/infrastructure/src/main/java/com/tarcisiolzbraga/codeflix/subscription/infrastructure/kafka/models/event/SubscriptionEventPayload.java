package com.tarcisiolzbraga.codeflix.subscription.infrastructure.kafka.models.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

// O que o consumidor precisa de dentro do `payload` de um evento de assinatura: de quem é a
// assinatura. O resto de cada evento não interessa a quem concede acesso, e ler só isto mantém o
// consumidor imune a campo novo em qualquer um deles.
@JsonIgnoreProperties(ignoreUnknown = true)
public record SubscriptionEventPayload(@JsonProperty("accountId") String accountId) {}
