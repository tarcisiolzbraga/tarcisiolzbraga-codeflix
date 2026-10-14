package com.tarcisiolzbraga.codeflix.subscription.infrastructure.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

// Quantos dias a assinatura fica incompleta antes de ser cancelada. Dois é o valor do curso, e o
// que ele representa é quanto tempo se dá a quem teve o cartão recusado para resolver.
@ConfigurationProperties("subscription")
public record SubscriptionProperties(Integer toleranceDays) {

    private static final int DEFAULT_TOLERANCE_DAYS = 2;

    public SubscriptionProperties {
        toleranceDays = toleranceDays == null ? DEFAULT_TOLERANCE_DAYS : toleranceDays;
        if (toleranceDays < 0) {
            throw new IllegalStateException("'subscription.tolerance-days' should not be negative");
        }
    }
}
