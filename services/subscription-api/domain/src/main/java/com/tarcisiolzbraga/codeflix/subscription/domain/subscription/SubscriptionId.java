package com.tarcisiolzbraga.codeflix.subscription.domain.subscription;

import com.tarcisiolzbraga.codeflix.subscription.domain.Identifier;
import com.tarcisiolzbraga.codeflix.subscription.domain.exceptions.DomainException;
import java.util.UUID;

public record SubscriptionId(UUID value) implements Identifier {

    public SubscriptionId {
        if (value == null) {
            throw DomainException.with("'subscriptionId' should not be null");
        }
    }

    public static SubscriptionId unique() {
        return new SubscriptionId(UUID.randomUUID());
    }

    public static SubscriptionId from(final String value) {
        return new SubscriptionId(Identifier.uuidOf(value, SubscriptionId.class));
    }
}
