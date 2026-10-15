package com.tarcisiolzbraga.codeflix.subscription.application.subscription.subscribe;

import com.tarcisiolzbraga.codeflix.subscription.application.account.CallerAccount;
import com.tarcisiolzbraga.codeflix.subscription.application.subscription.SubscriptionGateways;
import com.tarcisiolzbraga.codeflix.subscription.application.subscription.SubscriptionOutput;
import com.tarcisiolzbraga.codeflix.subscription.domain.exceptions.ConflictException;
import com.tarcisiolzbraga.codeflix.subscription.domain.exceptions.DomainException;
import com.tarcisiolzbraga.codeflix.subscription.domain.exceptions.NotFoundException;
import com.tarcisiolzbraga.codeflix.subscription.domain.plan.Plan;
import com.tarcisiolzbraga.codeflix.subscription.domain.plan.PlanId;
import com.tarcisiolzbraga.codeflix.subscription.domain.subscription.Subscription;
import com.tarcisiolzbraga.codeflix.subscription.domain.subscription.SubscriptionId;
import com.tarcisiolzbraga.codeflix.subscription.domain.subscription.SubscriptionStatus;
import java.util.Objects;

public class DefaultSubscribeUseCase extends SubscribeUseCase {

    private final SubscriptionGateways gateways;

    public DefaultSubscribeUseCase(final SubscriptionGateways gateways) {
        this.gateways = Objects.requireNonNull(gateways, "'gateways' should not be null");
    }

    @Override
    public SubscriptionOutput execute(final SubscribeCommand command) {
        Objects.requireNonNull(command, "'command' should not be null");
        final var account = CallerAccount.of(this.gateways.accounts(), command.userId());
        final var planId = PlanId.from(command.planId());

        final var plan = this.gateways
                .plans()
                .planOfId(planId)
                .orElseThrow(() -> NotFoundException.with(Plan.class, planId));

        // Plano fora de venda não se assina. A leitura do plano devolve o inativo de propósito —
        // quem administra precisa vê-lo —, então a recusa é aqui, e não na busca.
        if (!plan.active()) {
            throw DomainException.with("this plan is not on sale");
        }

        // Uma conta assina uma vez por vez. Cancelada não conta: reassinar é legítimo.
        this.gateways.subscriptions().latestSubscriptionOfAccount(account.id()).ifPresent(existing -> {
            if (existing.status() != SubscriptionStatus.CANCELED) {
                throw ConflictException.alreadySubscribed();
            }
        });

        final var subscription =
                Subscription.newSubscription(SubscriptionId.unique(), account.id(), planId);
        return SubscriptionOutput.from(this.gateways.subscriptions().save(subscription));
    }
}
