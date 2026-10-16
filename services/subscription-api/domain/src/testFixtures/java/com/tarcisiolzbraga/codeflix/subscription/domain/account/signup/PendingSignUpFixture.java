package com.tarcisiolzbraga.codeflix.subscription.domain.account.signup;

import com.tarcisiolzbraga.codeflix.subscription.domain.account.AccountFixture;
import com.tarcisiolzbraga.codeflix.subscription.domain.utils.InstantUtils;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

// Dados de teste do cadastro pendente, iguais em todo teste que não se importa com os valores.
public final class PendingSignUpFixture {

    private PendingSignUpFixture() {}

    public static Instant expiresTomorrow() {
        return InstantUtils.now().plus(1, ChronoUnit.DAYS);
    }

    public static PendingSignUp pendingSignUp() {
        return pendingSignUpWith(SignUpToken.generate());
    }

    public static PendingSignUp pendingSignUpWith(final SignUpToken token) {
        return PendingSignUp.newPendingSignUp(
                PendingSignUpId.unique(),
                AccountFixture.userId(),
                AccountFixture.email(),
                AccountFixture.name(),
                AccountFixture.document(),
                token,
                expiresTomorrow());
    }

    // Um pendente como ele volta do banco: com versão e já guardando só o resumo do token.
    public static PendingSignUp storedPendingSignUp(final SignUpToken token, final Instant expiresAt) {
        return PendingSignUp.with(
                PendingSignUpId.unique(),
                1,
                AccountFixture.userId(),
                AccountFixture.email(),
                AccountFixture.name(),
                AccountFixture.document(),
                token.hashed(),
                expiresAt,
                null);
    }
}
