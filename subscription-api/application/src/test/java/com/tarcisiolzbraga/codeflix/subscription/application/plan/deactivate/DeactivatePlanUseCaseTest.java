package com.tarcisiolzbraga.codeflix.subscription.application.plan.deactivate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tarcisiolzbraga.codeflix.subscription.domain.exceptions.DomainException;
import com.tarcisiolzbraga.codeflix.subscription.domain.exceptions.NotFoundException;
import com.tarcisiolzbraga.codeflix.subscription.domain.plan.Plan;
import com.tarcisiolzbraga.codeflix.subscription.domain.plan.PlanFixture;
import com.tarcisiolzbraga.codeflix.subscription.domain.plan.PlanGateway;
import com.tarcisiolzbraga.codeflix.subscription.domain.plan.PlanId;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DeactivatePlanUseCaseTest {

    @Mock
    private PlanGateway planGateway;

    @InjectMocks
    private DefaultDeactivatePlanUseCase useCase;

    @Captor
    private ArgumentCaptor<Plan> savedPlan;

    @Test
    void givenAPlanInTheOtherState_whenExecutes_thenSavesItInInactiveState() {
        final var stored = PlanFixture.plan();
        when(planGateway.planOfId(stored.id())).thenReturn(Optional.of(stored));
        when(planGateway.save(any())).thenAnswer(returnsFirstArg());

        useCase.execute(stored.id().getValue());

        verify(planGateway).save(savedPlan.capture());
        assertFalse(savedPlan.getValue().active());
    }

    @Test
    void givenAPlanAlreadyInThatState_whenExecutes_thenItStaysThereAndIsStillSaved() {
        final var stored = PlanFixture.inactivePlan();
        when(planGateway.planOfId(stored.id())).thenReturn(Optional.of(stored));
        when(planGateway.save(any())).thenAnswer(returnsFirstArg());

        useCase.execute(stored.id().getValue());

        verify(planGateway).save(savedPlan.capture());
        assertFalse(savedPlan.getValue().active());
    }

    @Test
    void givenAPlan_whenExecutes_thenReturnsItsCurrentState() {
        final var stored = PlanFixture.plan();
        when(planGateway.planOfId(stored.id())).thenReturn(Optional.of(stored));
        when(planGateway.save(any())).thenAnswer(returnsFirstArg());

        final var actual = useCase.execute(stored.id().getValue());

        assertEquals(stored.id().getValue(), actual.id());
    }

    @Test
    void givenAMissingPlan_whenExecutes_thenThrowsNotFoundAndNeverSaves() {
        final var anId = PlanId.unique();
        when(planGateway.planOfId(anId)).thenReturn(Optional.empty());

        final var actual = assertThrows(NotFoundException.class, () -> useCase.execute(anId.getValue()));

        assertEquals("Plan with id %s was not found".formatted(anId.getValue()), actual.getMessage());
        verify(planGateway, never()).save(any());
    }

    @Test
    void givenAnIdThatIsNotUuid_whenExecutes_thenThrowsAndNeverReachesTheGateway() {
        final var invalid = "nao-e-uuid";

        final var actual = assertThrows(DomainException.class, () -> useCase.execute(invalid));

        assertEquals("'nao-e-uuid' is not a valid PlanId", actual.getMessage());
        verify(planGateway, never()).planOfId(any());
    }
}
