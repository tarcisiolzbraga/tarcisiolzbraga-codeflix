package com.tarcisiolzbraga.codeflix.subscription.domain.subscription;

import com.tarcisiolzbraga.codeflix.subscription.domain.DomainEvent;
import com.tarcisiolzbraga.codeflix.subscription.domain.utils.InstantUtils;
import java.time.Instant;

// A cobrança passou e a assinatura andou um mês. Concede o catálogo de novo, de propósito: quem
// tinha perdido o acesso num cancelamento anterior volta a tê-lo sem caso especial.
//
// A conta vem no evento, e não só o id da assinatura: quem consome precisa saber de **quem** é a
// assinatura para mexer no acesso ao catálogo. Sem ela o consumidor teria de ler a assinatura de
// volta só para descobrir o dono, e leria um estado mais novo que o do fato que está processando.
public record SubscriptionRenewed(String aggregateId, String accountId, Instant occurredOn) implements DomainEvent {

    private static final String AGGREGATE_TYPE = "Subscription";

    public static SubscriptionRenewed from(final Subscription aSubscription) {
        return new SubscriptionRenewed(
                aSubscription.id().getValue(), aSubscription.accountId().getValue(), InstantUtils.now());
    }

    @Override
    public String aggregateType() {
        return AGGREGATE_TYPE;
    }
}
