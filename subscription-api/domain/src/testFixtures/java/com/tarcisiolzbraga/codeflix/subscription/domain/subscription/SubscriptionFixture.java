package com.tarcisiolzbraga.codeflix.subscription.domain.subscription;

import com.tarcisiolzbraga.codeflix.subscription.domain.account.AccountId;
import com.tarcisiolzbraga.codeflix.subscription.domain.plan.PlanId;
import com.tarcisiolzbraga.codeflix.subscription.domain.utils.InstantUtils;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

// Dados de teste da assinatura, iguais em todo teste que não se importa com os valores.
public final class SubscriptionFixture {

    public static final String TRANSACTION_ID = "tx-0001";

    private SubscriptionFixture() {}

    public static Subscription subscription() {
        return Subscription.newSubscription(SubscriptionId.unique(), AccountId.unique(), PlanId.unique());
    }

    // Uma assinatura como ela volta do banco: com versão, datas no passado e sem evento pendente.
    public static Subscription storedSubscription() {
        return storedSubscription(SubscriptionStatus.TRAILING);
    }

    public static Subscription storedSubscription(final SubscriptionStatus status) {
        return storedSubscription(status, LocalDate.of(2026, 11, 14));
    }

    public static Subscription storedSubscription(final SubscriptionStatus status, final LocalDate dueDate) {
        final var longAgo = InstantUtils.now().minus(30, ChronoUnit.DAYS);
        return Subscription.with(
                SubscriptionId.unique(),
                1,
                AccountId.unique(),
                PlanId.unique(),
                status,
                dueDate,
                null,
                null,
                longAgo,
                longAgo);
    }

    public static Instant longAgo() {
        return InstantUtils.now().minus(30, ChronoUnit.DAYS);
    }
}
