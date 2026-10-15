package com.tarcisiolzbraga.codeflix.subscription.domain.subscription;

import com.tarcisiolzbraga.codeflix.subscription.domain.account.AccountId;
import java.util.Optional;

public interface SubscriptionGateway {

    // A assinatura mais recente da conta. Uma conta assina uma vez por vez, mas pode reassinar
    // depois de cancelar — então o que interessa é a última, e não "a" assinatura.
    Optional<Subscription> latestSubscriptionOfAccount(AccountId anAccountId);

    Subscription save(Subscription aSubscription);
}
