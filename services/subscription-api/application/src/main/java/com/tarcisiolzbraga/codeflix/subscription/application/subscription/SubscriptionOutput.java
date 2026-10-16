package com.tarcisiolzbraga.codeflix.subscription.application.subscription;

import com.tarcisiolzbraga.codeflix.subscription.domain.subscription.Subscription;
import java.time.Instant;
import java.time.LocalDate;

// A assinatura como a aplicação a entrega. Os ids saem como texto: a borda é o JSON.
public record SubscriptionOutput(
        String id,
        String planId,
        String status,
        LocalDate dueDate,
        Instant lastRenewDate,
        Instant createdAt) {

    public static SubscriptionOutput from(final Subscription aSubscription) {
        return new SubscriptionOutput(
                aSubscription.id().getValue(),
                aSubscription.planId().getValue(),
                aSubscription.status().value(),
                aSubscription.dueDate(),
                aSubscription.lastRenewDate(),
                aSubscription.createdAt());
    }
}
