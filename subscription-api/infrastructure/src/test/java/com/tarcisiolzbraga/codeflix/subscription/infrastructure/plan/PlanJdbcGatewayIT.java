package com.tarcisiolzbraga.codeflix.subscription.infrastructure.plan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tarcisiolzbraga.codeflix.subscription.domain.exceptions.ConflictException;
import com.tarcisiolzbraga.codeflix.subscription.domain.money.Money;
import com.tarcisiolzbraga.codeflix.subscription.domain.plan.Plan;
import com.tarcisiolzbraga.codeflix.subscription.domain.plan.PlanCommand;
import com.tarcisiolzbraga.codeflix.subscription.domain.plan.PlanFixture;
import com.tarcisiolzbraga.codeflix.subscription.domain.plan.PlanId;
import com.tarcisiolzbraga.codeflix.subscription.infrastructure.IntegrationTest;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@IntegrationTest
class PlanJdbcGatewayIT {

    @Autowired
    private PlanJdbcGateway gateway;

    @Test
    void givenANewPlan_whenSavesIt_thenItCanBeReadBackWithTheSameValues() {
        final var plan = PlanFixture.plan();

        gateway.save(plan);

        final var actual = gateway.planOfId(plan.id()).orElseThrow();
        assertEquals(PlanFixture.NAME, actual.name());
    }

    // A razão de guardar BigDecimal e não Double: o valor tem de voltar do DECIMAL(15,2) idêntico.
    @Test
    void givenAPriceWithCents_whenSavesAndReadsItBack_thenTheAmountIsExactlyTheSame() {
        final var plan = PlanFixture.plan();

        gateway.save(plan);

        final var actual = gateway.planOfId(plan.id()).orElseThrow();
        assertEquals(PlanFixture.price(), actual.price());
    }

    @Test
    void givenANewPlan_whenSavesIt_thenTheRowIsStoredAtVersionOne() {
        final var plan = PlanFixture.plan();

        gateway.save(plan);

        final var actual = gateway.planOfId(plan.id()).orElseThrow();
        assertEquals(1, actual.version());
    }

    @Test
    void givenAnInactivePlan_whenSavesAndReadsItBack_thenItIsStillInactive() {
        final var plan = PlanFixture.inactivePlan();

        gateway.save(plan);

        final var actual = gateway.planOfId(plan.id()).orElseThrow();
        assertFalse(actual.active());
    }

    @Test
    void givenAMissingId_whenReadsIt_thenNothingComesBack() {
        final var anId = PlanId.unique();

        final var actual = gateway.planOfId(anId);

        assertTrue(actual.isEmpty());
    }

    @Test
    void givenAStoredPlan_whenChangesAndSavesIt_thenTheNewValuesAreStored() {
        final var stored = savedPlan();
        stored.execute(new PlanCommand.ChangePlan("Básico", "Uma tela", Money.of("BRL", new BigDecimal("19.90"))));

        gateway.save(stored);

        final var actual = gateway.planOfId(stored.id()).orElseThrow();
        assertEquals("Básico", actual.name());
    }

    @Test
    void givenAStoredPlan_whenSavesItAgain_thenTheVersionMovesForward() {
        final var stored = savedPlan();
        stored.execute(new PlanCommand.DeactivatePlan());

        gateway.save(stored);

        final var actual = gateway.planOfId(stored.id()).orElseThrow();
        assertEquals(2, actual.version());
    }

    @Test
    void givenAStoredPlan_whenUpdatesIt_thenCreatedAtIsNotRewritten() {
        final var stored = savedPlan();
        final var createdAt = stored.createdAt();
        stored.execute(new PlanCommand.DeactivatePlan());

        gateway.save(stored);

        assertEquals(createdAt, gateway.planOfId(stored.id()).orElseThrow().createdAt());
    }

    // O lock otimista: salvar duas vezes a mesma instância usa a versão antiga na segunda, que é
    // exatamente o que aconteceria se outro processo tivesse gravado no meio.
    @Test
    void givenAPlanAtAVersionThatIsNoLongerCurrent_whenSavesIt_thenRefusesWithConflict() {
        final var stored = savedPlan();
        stored.execute(new PlanCommand.DeactivatePlan());
        gateway.save(stored);

        final var actual = assertThrows(ConflictException.class, () -> gateway.save(stored));

        assertEquals(
                "Plan with id %s was changed by someone else; read it again".formatted(stored.id().getValue()),
                actual.getMessage());
    }

    @Test
    void givenANameAtTheColumnLimit_whenSavesIt_thenTheColumnTakesIt() {
        final var name = "a".repeat(100);
        final var plan = Plan.newPlan(PlanId.unique(), name, PlanFixture.DESCRIPTION, true, PlanFixture.price());

        gateway.save(plan);

        assertEquals(name, gateway.planOfId(plan.id()).orElseThrow().name());
    }

    // Grava e lê de volta, para o teste seguir com a versão que está no banco.
    private Plan savedPlan() {
        final var plan = PlanFixture.plan();
        gateway.save(plan);
        return gateway.planOfId(plan.id()).orElseThrow();
    }
}
