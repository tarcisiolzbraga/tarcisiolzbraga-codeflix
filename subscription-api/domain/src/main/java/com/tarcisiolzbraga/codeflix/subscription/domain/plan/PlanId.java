package com.tarcisiolzbraga.codeflix.subscription.domain.plan;

import com.tarcisiolzbraga.codeflix.subscription.domain.Identifier;
import com.tarcisiolzbraga.codeflix.subscription.domain.exceptions.DomainException;
import java.util.UUID;

// UUID, e não o auto-increment da referência. Com o id conhecido antes de inserir, dois remendos do
// curso deixam de ser necessários: o PlanId.empty(), que existia para representar "ainda sem id", e o
// Plan.withId(...), que devolvia um plano novo depois de o banco sortear a chave.
public record PlanId(UUID value) implements Identifier {

    public PlanId {
        if (value == null) {
            throw DomainException.with("'planId' should not be null");
        }
    }

    // Quem gera é o domínio, não o gateway: com UUID não há ida ao banco para saber o próximo id,
    // então o nextId() da referência perdeu a razão de existir.
    public static PlanId unique() {
        return new PlanId(UUID.randomUUID());
    }

    public static PlanId from(final String value) {
        return new PlanId(Identifier.uuidOf(value, PlanId.class));
    }
}
