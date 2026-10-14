package com.tarcisiolzbraga.codeflix.subscription.domain.account;

import com.tarcisiolzbraga.codeflix.subscription.domain.DomainEvent;
import com.tarcisiolzbraga.codeflix.subscription.domain.utils.InstantUtils;
import java.time.Instant;

// A conta existir é o fato que o resto do sistema espera: é por ele que o assinante entra no grupo
// de assinantes no provedor de identidade. Ninguém o consome ainda — a fila de saída é a fatia
// seguinte —, e o agregado já o registra para o consumidor não precisar de mudança no domínio.
public record AccountCreated(String aggregateId, Instant occurredOn) implements DomainEvent {

    private static final String AGGREGATE_TYPE = "Account";

    public static AccountCreated from(final Account anAccount) {
        return new AccountCreated(anAccount.id().getValue(), InstantUtils.now());
    }

    @Override
    public String aggregateType() {
        return AGGREGATE_TYPE;
    }
}
