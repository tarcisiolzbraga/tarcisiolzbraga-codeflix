package com.tarcisiolzbraga.codeflix.subscription.infrastructure.event;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

import com.tarcisiolzbraga.codeflix.subscription.domain.DomainEventPublisher;
import com.tarcisiolzbraga.codeflix.subscription.domain.account.AccountFixture;
import com.tarcisiolzbraga.codeflix.subscription.domain.plan.PlanFixture;
import com.tarcisiolzbraga.codeflix.subscription.domain.subscription.Subscription;
import com.tarcisiolzbraga.codeflix.subscription.domain.subscription.SubscriptionId;
import com.tarcisiolzbraga.codeflix.subscription.infrastructure.IntegrationTest;
import com.tarcisiolzbraga.codeflix.subscription.infrastructure.account.AccountJdbcGateway;
import com.tarcisiolzbraga.codeflix.subscription.infrastructure.plan.PlanJdbcGateway;
import com.tarcisiolzbraga.codeflix.subscription.infrastructure.subscription.SubscriptionJdbcGateway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

// Prova a transação do `save`, e só ela.
//
// Está separado do DomainEventOutboxIT porque precisa de um publisher que falha, e substituir esse
// bean troca o contexto do Spring — e porque este é o único teste que, tirando o @Transactional,
// fica vermelho. O outro passa com e sem, o que descobri à mão: lá a falha acontece **antes** de o
// evento ser gravado, então a ordem já garante o resultado e a transação não entra na conta.
//
// Com o publisher estourando depois do INSERT da assinatura, o pool entrega a conexão com
// auto-commit ligado: sem transação, a assinatura ficaria gravada e o evento perdido — uma
// assinatura viva sobre a qual ninguém nunca ficaria sabendo.
@IntegrationTest
class OutboxTransactionIT {

    @MockitoBean
    private DomainEventPublisher events;

    @Autowired
    private SubscriptionJdbcGateway subscriptions;

    @Autowired
    private AccountJdbcGateway accounts;

    @Autowired
    private PlanJdbcGateway plans;

    @Test
    void givenAPublisherThatFails_whenSavesASubscription_thenTheSubscriptionIsRolledBackToo() {
        // A conta e o plano são semeados ANTES de armar a falha: a conta também publica evento, e
        // com o publisher já estourando a montagem do teste morria antes do que ela quer exercitar.
        final var account = AccountFixture.account();
        this.accounts.save(account);
        final var plan = PlanFixture.plan();
        this.plans.save(plan);
        final var subscription = Subscription.newSubscription(SubscriptionId.unique(), account.id(), plan.id());

        doThrow(new IllegalStateException("a fila de saída caiu"))
                .when(this.events)
                .publishEvent(any());

        final var actual =
                assertThrows(IllegalStateException.class, () -> this.subscriptions.save(subscription));

        assertTrue(actual.getMessage().contains("a fila de saída caiu"));
        assertTrue(
                this.subscriptions.latestSubscriptionOfAccount(account.id()).isEmpty(),
                "a assinatura não deveria ter ficado gravada quando o evento falhou");
    }
}
