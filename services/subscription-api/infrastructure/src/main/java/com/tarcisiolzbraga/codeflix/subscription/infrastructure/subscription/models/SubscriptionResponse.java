package com.tarcisiolzbraga.codeflix.subscription.infrastructure.subscription.models;

import com.tarcisiolzbraga.codeflix.subscription.application.subscription.SubscriptionOutput;
import java.time.Instant;
import java.time.LocalDate;

public record SubscriptionResponse(
        String id, String planId, String status, LocalDate dueDate, Instant lastRenewDate, Instant createdAt) {

    public static SubscriptionResponse from(final SubscriptionOutput output) {
        return new SubscriptionResponse(
                output.id(),
                output.planId(),
                output.status(),
                output.dueDate(),
                output.lastRenewDate(),
                output.createdAt());
    }
}
