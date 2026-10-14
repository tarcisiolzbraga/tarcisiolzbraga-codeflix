package com.tarcisiolzbraga.codeflix.subscription.domain.account.signup;

import com.tarcisiolzbraga.codeflix.subscription.domain.Identifier;
import com.tarcisiolzbraga.codeflix.subscription.domain.exceptions.DomainException;
import java.util.UUID;

// O id do cadastro pendente, separado do token de propósito: o id identifica a linha e pode
// aparecer em log; o token é credencial e não pode.
public record PendingSignUpId(UUID value) implements Identifier {

    public PendingSignUpId {
        if (value == null) {
            throw DomainException.with("'pendingSignUpId' should not be null");
        }
    }

    public static PendingSignUpId unique() {
        return new PendingSignUpId(UUID.randomUUID());
    }

    public static PendingSignUpId from(final String value) {
        return new PendingSignUpId(Identifier.uuidOf(value, PendingSignUpId.class));
    }
}
