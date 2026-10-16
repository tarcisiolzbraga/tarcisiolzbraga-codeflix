package com.tarcisiolzbraga.codeflix.subscription.application.account.signup;

import com.tarcisiolzbraga.codeflix.subscription.domain.account.AccountGateway;
import com.tarcisiolzbraga.codeflix.subscription.domain.account.idp.IdentityProviderGateway;
import com.tarcisiolzbraga.codeflix.subscription.domain.account.signup.PendingSignUpGateway;
import com.tarcisiolzbraga.codeflix.subscription.domain.account.signup.SignUpNotifier;
import java.util.Objects;

// Os quatro colaboradores do cadastro, agrupados: a regra do projeto é no máximo quatro parâmetros
// por método, e o caso de uso ainda precisa do relógio e do prazo.
public record SignUpGateways(
        AccountGateway accounts,
        PendingSignUpGateway pendingSignUps,
        IdentityProviderGateway identityProvider,
        SignUpNotifier notifier) {

    public SignUpGateways {
        Objects.requireNonNull(accounts, "'accounts' should not be null");
        Objects.requireNonNull(pendingSignUps, "'pendingSignUps' should not be null");
        Objects.requireNonNull(identityProvider, "'identityProvider' should not be null");
        Objects.requireNonNull(notifier, "'notifier' should not be null");
    }
}
