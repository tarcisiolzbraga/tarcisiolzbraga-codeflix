package com.tarcisiolzbraga.codeflix.subscription.domain.account.signup;

import com.tarcisiolzbraga.codeflix.subscription.domain.person.Email;
import java.util.Optional;

public interface PendingSignUpGateway {

    // A busca é pelo resumo do token, não pelo token: é o resumo que está gravado.
    Optional<PendingSignUp> pendingSignUpOfToken(SignUpToken aToken);

    PendingSignUp save(PendingSignUp aPendingSignUp);

    // Pedir o cadastro de novo é legítimo — o e-mail pode não ter chegado. Isto garante que só o
    // link mais novo vale: os pendentes anteriores do mesmo e-mail saem antes de o novo entrar.
    int deleteUnconfirmedOf(Email anEmail);
}
