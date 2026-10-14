package com.tarcisiolzbraga.codeflix.subscription.domain.subscription;

import com.tarcisiolzbraga.codeflix.subscription.domain.DomainEvent;
import com.tarcisiolzbraga.codeflix.subscription.domain.utils.InstantUtils;
import java.time.Instant;

// a cobrança passou e o acesso continua.
public record SubscriptionRenewed(String aggregateId, Instant occurredOn) implements DomainEvent {

    private static final String AGGREGATE_TYPE = "Subscription";

    public static SubscriptionRenewed from(final Subscription aSubscription) {
        return new SubscriptionRenewed(aSubscription.id().getValue(), InstantUtils.now());
    }

    @Override
    public String aggregateType() {
        return AGGREGATE_TYPE;
    }
}
