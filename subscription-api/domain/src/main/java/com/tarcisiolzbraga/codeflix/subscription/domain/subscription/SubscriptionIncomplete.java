package com.tarcisiolzbraga.codeflix.subscription.domain.subscription;

import com.tarcisiolzbraga.codeflix.subscription.domain.DomainEvent;
import com.tarcisiolzbraga.codeflix.subscription.domain.utils.InstantUtils;
import java.time.Instant;

// A cobrança falhou e ainda há prazo. Carrega o motivo, que é o que o assinante precisa ler para
// resolver — cartão recusado e saldo insuficiente pedem ações diferentes.
public record SubscriptionIncomplete(String aggregateId, String reason, Instant occurredOn)
        implements DomainEvent {

    private static final String AGGREGATE_TYPE = "Subscription";

    public static SubscriptionIncomplete from(final Subscription aSubscription, final String reason) {
        return new SubscriptionIncomplete(aSubscription.id().getValue(), reason, InstantUtils.now());
    }

    @Override
    public String aggregateType() {
        return AGGREGATE_TYPE;
    }
}
