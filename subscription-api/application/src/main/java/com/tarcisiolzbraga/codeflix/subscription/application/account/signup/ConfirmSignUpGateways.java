package com.tarcisiolzbraga.codeflix.subscription.application.account.signup;

import com.tarcisiolzbraga.codeflix.subscription.domain.account.AccountGateway;
import com.tarcisiolzbraga.codeflix.subscription.domain.account.idp.IdentityProviderGateway;
import com.tarcisiolzbraga.codeflix.subscription.domain.account.signup.PendingSignUpGateway;
import java.util.Objects;

// Sem o notificador: confirmar não manda mensagem nenhuma. É por isso que não reaproveita o
// SignUpGateways — colaborador que não se usa é peso morto na assinatura.
public record ConfirmSignUpGateways(
        AccountGateway accounts,
        PendingSignUpGateway pendingSignUps,
        IdentityProviderGateway identityProvider) {

    public ConfirmSignUpGateways {
        Objects.requireNonNull(accounts, "'accounts' should not be null");
        Objects.requireNonNull(pendingSignUps, "'pendingSignUps' should not be null");
        Objects.requireNonNull(identityProvider, "'identityProvider' should not be null");
    }
}
