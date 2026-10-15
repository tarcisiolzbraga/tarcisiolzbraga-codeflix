package com.tarcisiolzbraga.codeflix.subscription.application.subscription.cancel;

import com.tarcisiolzbraga.codeflix.subscription.application.subscription.CallerSubscription;
import com.tarcisiolzbraga.codeflix.subscription.application.subscription.SubscriptionGateways;
import com.tarcisiolzbraga.codeflix.subscription.application.subscription.SubscriptionOutput;
import com.tarcisiolzbraga.codeflix.subscription.domain.subscription.SubscriptionCommand;
import java.util.Objects;

// Cancelar vale de qualquer estado, e cancelar o que já está cancelado não é erro: quem pede duas
// vezes não precisa ver um erro para saber que está cancelado.
public class DefaultCancelSubscriptionUseCase extends CancelSubscriptionUseCase {

    private final SubscriptionGateways gateways;

    public DefaultCancelSubscriptionUseCase(final SubscriptionGateways gateways) {
        this.gateways = Objects.requireNonNull(gateways, "'gateways' should not be null");
    }

    @Override
    public SubscriptionOutput execute(final String userId) {
        final var subscription = CallerSubscription.of(this.gateways, userId);
        subscription.execute(new SubscriptionCommand.Cancel());
        return SubscriptionOutput.from(this.gateways.subscriptions().save(subscription));
    }
}
