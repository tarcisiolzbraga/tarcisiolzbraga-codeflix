package com.tarcisiolzbraga.codeflix.subscription.domain.account.signup;

import java.util.Optional;

public interface PendingSignUpGateway {

    // A busca é pelo resumo do token, não pelo token: é o resumo que está gravado.
    Optional<PendingSignUp> pendingSignUpOfToken(SignUpToken aToken);

    PendingSignUp save(PendingSignUp aPendingSignUp);
}
