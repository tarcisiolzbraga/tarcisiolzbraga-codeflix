package com.tarcisiolzbraga.codeflix.subscription.application.subscription;

import com.tarcisiolzbraga.codeflix.subscription.domain.account.AccountGateway;
import com.tarcisiolzbraga.codeflix.subscription.domain.plan.PlanGateway;
import com.tarcisiolzbraga.codeflix.subscription.domain.subscription.SubscriptionGateway;
import java.util.Objects;

// Os três colaboradores que toda operação de assinatura usa, agrupados: a regra do projeto é no
// máximo quatro parâmetros por método, e a cobrança ainda precisa do provedor de pagamento, do
// relógio e do prazo de tolerância.
public record SubscriptionGateways(
        AccountGateway accounts, PlanGateway plans, SubscriptionGateway subscriptions) {

    public SubscriptionGateways {
        Objects.requireNonNull(accounts, "'accounts' should not be null");
        Objects.requireNonNull(plans, "'plans' should not be null");
        Objects.requireNonNull(subscriptions, "'subscriptions' should not be null");
    }
}
