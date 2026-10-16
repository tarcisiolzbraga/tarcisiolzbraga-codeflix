package com.tarcisiolzbraga.codeflix.subscription.application.plan.get;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tarcisiolzbraga.codeflix.subscription.domain.exceptions.DomainException;
import com.tarcisiolzbraga.codeflix.subscription.domain.exceptions.NotFoundException;
import com.tarcisiolzbraga.codeflix.subscription.domain.plan.PlanFixture;
import com.tarcisiolzbraga.codeflix.subscription.domain.plan.PlanGateway;
import com.tarcisiolzbraga.codeflix.subscription.domain.plan.PlanId;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GetPlanByIdUseCaseTest {

    @Mock
    private PlanGateway planGateway;

    @InjectMocks
    private DefaultGetPlanByIdUseCase useCase;

    @Test
    void givenAnExistingPlan_whenExecutes_thenReturnsItsValues() {
        final var stored = PlanFixture.storedPlan();
        when(planGateway.planOfId(stored.id())).thenReturn(Optional.of(stored));

        final var actual = useCase.execute(stored.id().getValue());

        assertEquals(PlanFixture.NAME, actual.name());
    }

    @Test
    void givenAnExistingPlan_whenExecutes_thenReturnsThePriceAndTheCurrency() {
        final var stored = PlanFixture.storedPlan();
        when(planGateway.planOfId(stored.id())).thenReturn(Optional.of(stored));

        final var actual = useCase.execute(stored.id().getValue());

        assertEquals(PlanFixture.AMOUNT, actual.price());
        assertEquals(PlanFixture.CURRENCY, actual.currency());
    }

    // Plano fora de venda continua sendo devolvido: é o operador que administra esse estado, e
    // esconder o inativo tiraria o caminho de encontrar um para reativar.
    @Test
    void givenAnInactivePlan_whenExecutes_thenItIsStillReturned() {
        final var stored = PlanFixture.inactivePlan();
        when(planGateway.planOfId(stored.id())).thenReturn(Optional.of(stored));

        final var actual = useCase.execute(stored.id().getValue());

        assertFalse(actual.active());
    }

    @Test
    void givenAMissingPlan_whenExecutes_thenThrowsNotFound() {
        final var anId = PlanId.unique();
        when(planGateway.planOfId(anId)).thenReturn(Optional.empty());

        final var actual = assertThrows(NotFoundException.class, () -> useCase.execute(anId.getValue()));

        assertEquals("Plan with id %s was not found".formatted(anId.getValue()), actual.getMessage());
    }

    @Test
    void givenAnIdThatIsNotUuid_whenExecutes_thenThrowsAndNeverReachesTheGateway() {
        final var invalid = "7";

        final var actual = assertThrows(DomainException.class, () -> useCase.execute(invalid));

        assertEquals("'7' is not a valid PlanId", actual.getMessage());
        verify(planGateway, never()).planOfId(any());
    }
}
