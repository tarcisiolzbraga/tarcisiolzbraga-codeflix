package com.tarcisiolzbraga.codeflix.subscription.application.subscription;

import com.tarcisiolzbraga.codeflix.subscription.application.account.CallerAccount;
import com.tarcisiolzbraga.codeflix.subscription.domain.account.Account;
import com.tarcisiolzbraga.codeflix.subscription.domain.exceptions.NotFoundException;
import com.tarcisiolzbraga.codeflix.subscription.domain.subscription.Subscription;

// Achar a conta de quem chama e a assinatura dela. Os três casos de uso que mexem numa assinatura
// existente começam por isto, e a mensagem de "não achei" tem de ser a mesma nos três.
public final class CallerSubscription {

    private CallerSubscription() {}

    public static Account account(final SubscriptionGateways gateways, final String userId) {
        return CallerAccount.of(gateways.accounts(), userId);
    }

    public static Subscription of(final SubscriptionGateways gateways, final String userId) {
        final var account = account(gateways, userId);
        return gateways.subscriptions()
                .latestSubscriptionOfAccount(account.id())
                .orElseThrow(NotFoundException::callerSubscription);
    }
}
