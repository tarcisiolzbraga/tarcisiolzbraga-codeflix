package com.tarcisiolzbraga.codeflix.subscription.infrastructure.subscription.models;

import com.tarcisiolzbraga.codeflix.subscription.application.subscription.charge.ChargeSubscriptionOutput;

// A assinatura depois da cobrança, e o que aconteceu na tentativa.
//
// Responde 200 mesmo quando a cobrança é recusada, e isso é deliberado: a requisição foi atendida e
// o sistema fez o que devia — quem recusou foi o provedor. O resultado está no corpo, com o motivo,
// que é o que o cliente precisa mostrar para a pessoa resolver.
public record ChargeResponse(
        SubscriptionResponse subscription, boolean charged, String transactionId, String reason) {

    public static ChargeResponse from(final ChargeSubscriptionOutput output) {
        return new ChargeResponse(
                SubscriptionResponse.from(output.subscription()),
                output.charged(),
                output.transactionId(),
                output.reason());
    }
}
