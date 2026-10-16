package com.tarcisiolzbraga.codeflix.subscription.application.subscription.get;

import com.tarcisiolzbraga.codeflix.subscription.application.subscription.CallerSubscription;
import com.tarcisiolzbraga.codeflix.subscription.application.subscription.SubscriptionGateways;
import com.tarcisiolzbraga.codeflix.subscription.application.subscription.SubscriptionOutput;
import java.util.Objects;

public class DefaultGetMySubscriptionUseCase extends GetMySubscriptionUseCase {

    private final SubscriptionGateways gateways;

    public DefaultGetMySubscriptionUseCase(final SubscriptionGateways gateways) {
        this.gateways = Objects.requireNonNull(gateways, "'gateways' should not be null");
    }

    @Override
    public SubscriptionOutput execute(final String userId) {
        return SubscriptionOutput.from(CallerSubscription.of(this.gateways, userId));
    }
}
