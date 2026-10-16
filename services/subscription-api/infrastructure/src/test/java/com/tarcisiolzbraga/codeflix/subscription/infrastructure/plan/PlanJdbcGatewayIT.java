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
import com.tarcisiolzbraga.codeflix.subscription.domain.pagination.PageRequest;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@IntegrationTest
class PlanJdbcGatewayIT {

    @Autowired
    private PlanJdbcGateway gateway;

    // O catálogo aberto mostra só o que está à venda. Um plano fora de venda aparecendo ali seria
    // oferecer o que não se pode assinar.
    @Test
    void givenActiveAndInactivePlans_whenAsksForTheOnesOnSale_thenOnlyTheActiveComeBack() {
        gateway.save(PlanFixture.planWithName("A venda"));
        gateway.save(PlanFixture.activePlanWithName("A venda tambem"));
        final var actual = gateway.activePlansOfPage(new PageRequest(0, 10));

        assertEquals(1, actual.total());
        assertEquals("A venda tambem", actual.items().getFirst().name());
    }

    @Test
    void givenActiveAndInactivePlans_whenTheOperatorLists_thenBothComeBack() {
        gateway.save(PlanFixture.planWithName("Fora de venda"));
        gateway.save(PlanFixture.activePlanWithName("A venda"));

        final var actual = gateway.plansOfPage(new PageRequest(0, 10));

        assertEquals(2, actual.total());
    }

    // O total é o de registros que casam, não o da página: é dele que sai o número de páginas, e sem
    // ele o cliente não sabe se há mais.
    @Test
    void givenMorePlansThanThePage_whenLists_thenTheTotalCountsAllAndThePageHoldsThePerPage() {
        for (var i = 0; i < 5; i++) {
            gateway.save(PlanFixture.activePlanWithName("Plano " + i));
        }

        final var actual = gateway.plansOfPage(new PageRequest(0, 2));

        assertEquals(5, actual.total());
        assertEquals(2, actual.items().size());
        assertEquals(0, actual.currentPage());
        assertEquals(2, actual.perPage());
    }

    @Test
    void givenSeveralPlans_whenAsksForTheSecondPage_thenItDoesNotRepeatTheFirst() {
        for (var i = 0; i < 5; i++) {
            gateway.save(PlanFixture.activePlanWithName("Plano " + i));
        }

        final var first = gateway.plansOfPage(new PageRequest(0, 2));
        final var second = gateway.plansOfPage(new PageRequest(1, 2));

        assertTrue(
                first.items().stream()
                        .noneMatch(a -> second.items().stream().anyMatch(b -> b.id().equals(a.id()))),
                "as duas páginas não deveriam repetir ninguém");
    }

    // Ordenada pelo nome: é a ordem que uma tela de escolha mostra, e é o que torna a paginação
    // estável entre consultas.
    @Test
    void givenPlansOutOfOrder_whenLists_thenTheyComeSortedByName() {
        gateway.save(PlanFixture.activePlanWithName("Zeta"));
        gateway.save(PlanFixture.activePlanWithName("Alfa"));
        gateway.save(PlanFixture.activePlanWithName("Meio"));

        final var actual = gateway.plansOfPage(new PageRequest(0, 10));

        assertEquals(
                List.of("Alfa", "Meio", "Zeta"), actual.items().stream().map(it -> it.name()).toList());
    }

    @Test
    void givenNoPlans_whenLists_thenThePageIsEmptyAndTheTotalIsZero() {
        final var actual = gateway.plansOfPage(new PageRequest(0, 10));

        assertEquals(0, actual.total());
        assertTrue(actual.items().isEmpty());
    }

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
