package com.tarcisiolzbraga.codeflix.subscription.domain.plan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tarcisiolzbraga.codeflix.subscription.domain.exceptions.DomainException;
import com.tarcisiolzbraga.codeflix.subscription.domain.money.Money;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class PlanTest {

    private static final String NAME = PlanFixture.NAME;
    private static final String DESCRIPTION = PlanFixture.DESCRIPTION;

    @Test
    void givenValidData_whenCreatesANewPlan_thenHoldsTheGivenValues() {
        final var id = PlanId.unique();

        final var actual = Plan.newPlan(id, NAME, DESCRIPTION, true, PlanFixture.price());

        assertEquals(id, actual.id());
    }

    @Test
    void givenValidData_whenCreatesANewPlan_thenItStartsAtVersionZeroBecauseItWasNeverStored() {
        final var id = PlanId.unique();

        final var actual = Plan.newPlan(id, NAME, DESCRIPTION, true, PlanFixture.price());

        assertEquals(0, actual.version());
    }

    @Test
    void givenValidData_whenCreatesANewPlan_thenCreatedAtAndUpdatedAtAreTheSameMoment() {
        final var id = PlanId.unique();

        final var actual = Plan.newPlan(id, NAME, DESCRIPTION, true, PlanFixture.price());

        assertEquals(actual.createdAt(), actual.updatedAt());
    }

    @Test
    void givenInactiveFlag_whenCreatesANewPlan_thenItIsInactive() {
        final var id = PlanId.unique();

        final var actual = Plan.newPlan(id, NAME, DESCRIPTION, false, PlanFixture.price());

        assertFalse(actual.active());
    }

    @Test
    void givenBlankName_whenCreatesANewPlan_thenThrowsDomainException() {
        final var name = "   ";

        final var actual = assertThrows(DomainException.class, () -> newPlanWithName(name));

        assertEquals("'name' should not be empty", actual.getMessage());
    }

    @Test
    void givenNameLongerThanAHundred_whenCreatesANewPlan_thenThrowsDomainException() {
        final var name = "a".repeat(101);

        final var actual = assertThrows(DomainException.class, () -> newPlanWithName(name));

        assertEquals("'name' should not have more than 100 characters", actual.getMessage());
    }

    @Test
    void givenBlankDescription_whenCreatesANewPlan_thenThrowsDomainException() {
        final var description = "";

        final var actual = assertThrows(DomainException.class, () -> newPlanWithDescription(description));

        assertEquals("'description' should not be empty", actual.getMessage());
    }

    @Test
    void givenDescriptionLongerThanFiveHundred_whenCreatesANewPlan_thenThrowsDomainException() {
        final var description = "a".repeat(501);

        final var actual = assertThrows(DomainException.class, () -> newPlanWithDescription(description));

        assertEquals("'description' should not have more than 500 characters", actual.getMessage());
    }

    @Test
    void givenNullPrice_whenCreatesANewPlan_thenThrowsDomainException() {
        final Money price = null;

        final var actual =
                assertThrows(DomainException.class, () -> Plan.newPlan(PlanId.unique(), NAME, DESCRIPTION, true, price));

        assertEquals("'price' should not be null", actual.getMessage());
    }

    @Test
    void givenNullActive_whenCreatesANewPlan_thenThrowsDomainException() {
        final Boolean active = null;

        final var actual = assertThrows(
                DomainException.class,
                () -> Plan.newPlan(PlanId.unique(), NAME, DESCRIPTION, active, PlanFixture.price()));

        assertEquals("'active' should not be null", actual.getMessage());
    }

    private static Plan newPlanWithName(final String name) {
        return Plan.newPlan(PlanId.unique(), name, DESCRIPTION, true, PlanFixture.price());
    }

    private static Plan newPlanWithDescription(final String description) {
        return Plan.newPlan(PlanId.unique(), NAME, description, true, PlanFixture.price());
    }

    @Test
    void givenAStoredPlan_whenChangesIt_thenTheNameIsTheNewOne() {
        final var plan = PlanFixture.storedPlan();

        plan.execute(new PlanCommand.ChangePlan("Básico", DESCRIPTION, otherPrice()));

        assertEquals("Básico", plan.name());
    }

    @Test
    void givenAStoredPlan_whenChangesIt_thenThePriceIsTheNewOne() {
        final var plan = PlanFixture.storedPlan();

        plan.execute(new PlanCommand.ChangePlan(NAME, DESCRIPTION, otherPrice()));

        assertEquals(otherPrice(), plan.price());
    }

    // Mudar dados não decide ativação: para isso existem os comandos próprios.
    @Test
    void givenAnActivePlan_whenChangesItsData_thenItStaysActive() {
        final var plan = PlanFixture.storedPlan();

        plan.execute(new PlanCommand.ChangePlan(NAME, DESCRIPTION, otherPrice()));

        assertTrue(plan.active());
    }

    // A versão é do lock otimista, e quem a incrementa é o UPDATE do repositório, na cláusula
    // WHERE version = :version. Se o agregado a incrementasse aqui, o WHERE procuraria uma versão
    // que o banco ainda não tem e nenhuma linha casaria — é o defeito que a referência do curso tem.
    @Test
    void givenAStoredPlan_whenChangesIt_thenTheVersionIsUntouched() {
        final var plan = PlanFixture.storedPlan(7);

        plan.execute(new PlanCommand.ChangePlan(NAME, DESCRIPTION, otherPrice()));

        assertEquals(7, plan.version());
    }

    @Test
    void givenAStoredPlan_whenChangesIt_thenUpdatedAtMovesForward() {
        final var plan = PlanFixture.storedPlan();
        final var before = plan.updatedAt();

        plan.execute(new PlanCommand.ChangePlan(NAME, DESCRIPTION, otherPrice()));

        assertTrue(plan.updatedAt().isAfter(before));
    }

    @Test
    void givenAStoredPlan_whenChangesIt_thenCreatedAtIsUntouched() {
        final var plan = PlanFixture.storedPlan();
        final var before = plan.createdAt();

        plan.execute(new PlanCommand.ChangePlan(NAME, DESCRIPTION, otherPrice()));

        assertEquals(before, plan.createdAt());
    }

    @Test
    void givenAStoredPlan_whenChangesItWithABlankName_thenThrowsDomainException() {
        final var plan = PlanFixture.storedPlan();
        final var command = new PlanCommand.ChangePlan("  ", DESCRIPTION, otherPrice());

        final var actual = assertThrows(DomainException.class, () -> plan.execute(command));

        assertEquals("'name' should not be empty", actual.getMessage());
    }

    @Test
    void givenAnInactivePlan_whenActivatesIt_thenItIsActive() {
        final var plan = PlanFixture.inactivePlan();

        plan.execute(new PlanCommand.ActivatePlan());

        assertTrue(plan.active());
    }

    @Test
    void givenAnActivePlan_whenDeactivatesIt_thenItIsInactive() {
        final var plan = PlanFixture.storedPlan();

        plan.execute(new PlanCommand.DeactivatePlan());

        assertFalse(plan.active());
    }

    @Test
    void givenAStoredPlan_whenDeactivatesIt_thenUpdatedAtMovesForward() {
        final var plan = PlanFixture.storedPlan();
        final var before = plan.updatedAt();

        plan.execute(new PlanCommand.DeactivatePlan());

        assertTrue(plan.updatedAt().isAfter(before));
    }

    @Test
    void givenAStoredPlan_whenExecutesNoCommand_thenUpdatedAtIsUntouched() {
        final var plan = PlanFixture.storedPlan();
        final var before = plan.updatedAt();

        plan.execute();

        assertEquals(before, plan.updatedAt());
    }

    @Test
    void givenAStoredPlan_whenExecutesNull_thenNothingHappens() {
        final var plan = PlanFixture.storedPlan();
        final var before = plan.updatedAt();

        plan.execute((PlanCommand[]) null);

        assertEquals(before, plan.updatedAt());
    }

    @Test
    void givenAPlanComingFromStorage_whenRebuiltWithIts_thenTheVersionIsPreserved() {
        final var longAgo = PlanFixture.longAgo();

        final var actual =
                Plan.with(PlanId.unique(), 12, NAME, DESCRIPTION, true, PlanFixture.price(), longAgo, longAgo);

        assertEquals(12, actual.version());
    }

    private static Money otherPrice() {
        return Money.of("BRL", new BigDecimal("99.90"));
    }
}
