package com.tarcisiolzbraga.codeflix.subscription.domain.subscription;

import com.tarcisiolzbraga.codeflix.subscription.domain.DomainEvent;
import com.tarcisiolzbraga.codeflix.subscription.domain.utils.InstantUtils;
import java.time.Instant;

// a assinatura passou a existir, e é por ele que o assinante entra no grupo do provedor.
public record SubscriptionCreated(String aggregateId, Instant occurredOn) implements DomainEvent {

    private static final String AGGREGATE_TYPE = "Subscription";

    public static SubscriptionCreated from(final Subscription aSubscription) {
        return new SubscriptionCreated(aSubscription.id().getValue(), InstantUtils.now());
    }

    @Override
    public String aggregateType() {
        return AGGREGATE_TYPE;
    }
}
