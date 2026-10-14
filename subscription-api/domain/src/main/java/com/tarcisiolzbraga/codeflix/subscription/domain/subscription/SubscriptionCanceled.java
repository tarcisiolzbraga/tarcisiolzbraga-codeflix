package com.tarcisiolzbraga.codeflix.subscription.domain.subscription;

import com.tarcisiolzbraga.codeflix.subscription.domain.DomainEvent;
import com.tarcisiolzbraga.codeflix.subscription.domain.utils.InstantUtils;
import java.time.Instant;

// a assinatura encerrou, e é por ele que o acesso ao catálogo sai.
public record SubscriptionCanceled(String aggregateId, Instant occurredOn) implements DomainEvent {

    private static final String AGGREGATE_TYPE = "Subscription";

    public static SubscriptionCanceled from(final Subscription aSubscription) {
        return new SubscriptionCanceled(aSubscription.id().getValue(), InstantUtils.now());
    }

    @Override
    public String aggregateType() {
        return AGGREGATE_TYPE;
    }
}
