package com.tarcisiolzbraga.codeflix.subscription.infrastructure.account.models;

import com.tarcisiolzbraga.codeflix.subscription.application.account.signup.ConfirmSignUpOutput;

public record ConfirmSignUpResponse(String accountId) {

    public static ConfirmSignUpResponse from(final ConfirmSignUpOutput output) {
        return new ConfirmSignUpResponse(output.accountId());
    }
}
