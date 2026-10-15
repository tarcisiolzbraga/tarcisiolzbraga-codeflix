package com.tarcisiolzbraga.codeflix.subscription.domain.account;

import com.tarcisiolzbraga.codeflix.subscription.domain.DomainEvent;
import com.tarcisiolzbraga.codeflix.subscription.domain.utils.InstantUtils;
import java.time.Instant;

// A conta existir é o fato, e ele é publicado para quem vier a se interessar — não para conceder
// acesso: a role de conta é concedida pela própria confirmação do cadastro, em linha, porque sem
// ela a pessoa não alcançaria nem a rota de assinar e não se pode fazer isso depender de o CDC
// estar de pé. Quem depende de evento é o catálogo, que vem da assinatura.
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
