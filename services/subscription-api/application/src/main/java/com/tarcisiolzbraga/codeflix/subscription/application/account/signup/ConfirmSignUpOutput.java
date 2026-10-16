package com.tarcisiolzbraga.codeflix.subscription.application.account.signup;

import com.tarcisiolzbraga.codeflix.subscription.domain.account.Account;

public record ConfirmSignUpOutput(String accountId) {

    public static ConfirmSignUpOutput from(final Account anAccount) {
        return new ConfirmSignUpOutput(anAccount.id().getValue());
    }
}
