package com.tarcisiolzbraga.codeflix.subscription.domain.subscription;

import com.tarcisiolzbraga.codeflix.subscription.domain.DomainEvent;
import com.tarcisiolzbraga.codeflix.subscription.domain.utils.InstantUtils;
import java.time.Instant;

// A cobrança foi recusada e ainda há prazo. **Não mexe no catálogo**: a tolerância existe para a
// pessoa resolver o cartão sem perder o serviço no meio, e passado o prazo vem o cancelamento, que
// é quem revoga.
//
// A conta vem no evento, e não só o id da assinatura: quem consome precisa saber de **quem** é a
// assinatura para mexer no acesso ao catálogo. Sem ela o consumidor teria de ler a assinatura de
// volta só para descobrir o dono, e leria um estado mais novo que o do fato que está processando.
public record SubscriptionIncomplete(String aggregateId, String accountId, String reason, Instant occurredOn)
        implements DomainEvent {

    private static final String AGGREGATE_TYPE = "Subscription";

    public static SubscriptionIncomplete from(final Subscription aSubscription, final String reason) {
        return new SubscriptionIncomplete(
                aSubscription.id().getValue(), aSubscription.accountId().getValue(), reason, InstantUtils.now());
    }

    @Override
    public String aggregateType() {
        return AGGREGATE_TYPE;
    }
}
