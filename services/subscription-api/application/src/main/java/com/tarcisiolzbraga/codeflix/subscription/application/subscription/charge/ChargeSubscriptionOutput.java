package com.tarcisiolzbraga.codeflix.subscription.application.subscription.charge;

import com.tarcisiolzbraga.codeflix.subscription.application.subscription.SubscriptionOutput;
import com.tarcisiolzbraga.codeflix.subscription.domain.subscription.Subscription;

// A assinatura depois da cobrança, e o que aconteceu na tentativa. O motivo vem nulo quando passou.
public record ChargeSubscriptionOutput(
        SubscriptionOutput subscription, boolean charged, String transactionId, String reason) {

    public static ChargeSubscriptionOutput succeeded(final Subscription aSubscription, final String transactionId) {
        return new ChargeSubscriptionOutput(SubscriptionOutput.from(aSubscription), true, transactionId, null);
    }

    public static ChargeSubscriptionOutput failed(
            final Subscription aSubscription, final String transactionId, final String reason) {
        return new ChargeSubscriptionOutput(SubscriptionOutput.from(aSubscription), false, transactionId, reason);
    }

    // Não havia o que cobrar: a assinatura não está vencida.
    public static ChargeSubscriptionOutput nothingDue(final Subscription aSubscription) {
        return new ChargeSubscriptionOutput(SubscriptionOutput.from(aSubscription), false, null, null);
    }
}
