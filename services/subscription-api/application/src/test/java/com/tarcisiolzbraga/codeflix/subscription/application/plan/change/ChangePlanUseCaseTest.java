package com.tarcisiolzbraga.codeflix.subscription.application.plan.change;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tarcisiolzbraga.codeflix.subscription.domain.exceptions.DomainException;
import com.tarcisiolzbraga.codeflix.subscription.domain.exceptions.NotFoundException;
import com.tarcisiolzbraga.codeflix.subscription.domain.money.Money;
import com.tarcisiolzbraga.codeflix.subscription.domain.plan.Plan;
import com.tarcisiolzbraga.codeflix.subscription.domain.plan.PlanFixture;
import com.tarcisiolzbraga.codeflix.subscription.domain.plan.PlanGateway;
import com.tarcisiolzbraga.codeflix.subscription.domain.plan.PlanId;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ChangePlanUseCaseTest {

    private static final String NEW_NAME = "Básico";
    private static final String NEW_DESCRIPTION = "Uma tela, sem download";
    private static final String CURRENCY = PlanFixture.CURRENCY;
    private static final BigDecimal NEW_PRICE = new BigDecimal("19.90");

    @Mock
    private PlanGateway planGateway;

    @InjectMocks
    private DefaultChangePlanUseCase useCase;

    @Captor
    private ArgumentCaptor<Plan> savedPlan;

    @Test
    void givenAnExistingPlan_whenExecutes_thenReturnsItsId() {
        final var stored = PlanFixture.storedPlan();
        when(planGateway.planOfId(stored.id())).thenReturn(Optional.of(stored));
        when(planGateway.save(any())).thenAnswer(returnsFirstArg());

        final var actual = useCase.execute(commandFor(stored));

        assertEquals(stored.id().getValue(), actual.id());
    }

    // O ponto do teste: o comando precisa ser aplicado ANTES de salvar. Sem o execute no agregado,
    // o caso de uso salvaria o plano como ele veio do banco e o teste continuaria passando se só
    // verificasse que save foi chamado.
    @Test
    void givenAnExistingPlan_whenExecutes_thenSavesThePlanAlreadyChanged() {
        final var stored = PlanFixture.storedPlan();
        when(planGateway.planOfId(stored.id())).thenReturn(Optional.of(stored));
        when(planGateway.save(any())).thenAnswer(returnsFirstArg());

        useCase.execute(commandFor(stored));

        verify(planGateway).save(savedPlan.capture());
        assertEquals(NEW_NAME, savedPlan.getValue().name());
    }

    @Test
    void givenAnExistingPlan_whenExecutes_thenSavesTheNewPrice() {
        final var stored = PlanFixture.storedPlan();
        when(planGateway.planOfId(stored.id())).thenReturn(Optional.of(stored));
        when(planGateway.save(any())).thenAnswer(returnsFirstArg());

        useCase.execute(commandFor(stored));

        verify(planGateway).save(savedPlan.capture());
        assertEquals(Money.of(CURRENCY, NEW_PRICE), savedPlan.getValue().price());
    }

    // Mudar dados não decide ativação: quem faz isso tem caso de uso próprio.
    @Test
    void givenAnActivePlan_whenExecutes_thenItStaysActive() {
        final var stored = PlanFixture.storedPlan();
        when(planGateway.planOfId(stored.id())).thenReturn(Optional.of(stored));
        when(planGateway.save(any())).thenAnswer(returnsFirstArg());

        useCase.execute(commandFor(stored));

        verify(planGateway).save(savedPlan.capture());
        assertTrue(savedPlan.getValue().active());
    }

    // A versão é a que veio do banco: é com ela que o repositório monta o WHERE do lock otimista.
    @Test
    void givenAStoredPlan_whenExecutes_thenTheVersionSentToTheGatewayIsTheStoredOne() {
        final var stored = PlanFixture.storedPlan(5);
        when(planGateway.planOfId(stored.id())).thenReturn(Optional.of(stored));
        when(planGateway.save(any())).thenAnswer(returnsFirstArg());

        useCase.execute(commandFor(stored));

        verify(planGateway).save(savedPlan.capture());
        assertEquals(5, savedPlan.getValue().version());
    }

    @Test
    void givenAMissingPlan_whenExecutes_thenThrowsNotFoundAndNeverSaves() {
        final var anId = PlanId.unique();
        when(planGateway.planOfId(anId)).thenReturn(Optional.empty());

        final var actual = assertThrows(NotFoundException.class, () -> useCase.execute(commandFor(anId)));

        assertEquals("Plan with id %s was not found".formatted(anId.getValue()), actual.getMessage());
        verify(planGateway, never()).save(any());
    }

    @Test
    void givenAnIdThatIsNotUuid_whenExecutes_thenThrowsAndNeverReachesTheGateway() {
        final var command = ChangePlanCommand.with("7", NEW_NAME, NEW_DESCRIPTION, CURRENCY, NEW_PRICE);

        final var actual = assertThrows(DomainException.class, () -> useCase.execute(command));

        assertEquals("'7' is not a valid PlanId", actual.getMessage());
        verify(planGateway, never()).planOfId(any());
    }

    @Test
    void givenABlankName_whenExecutes_thenThrowsAndNeverSaves() {
        final var stored = PlanFixture.storedPlan();
        when(planGateway.planOfId(stored.id())).thenReturn(Optional.of(stored));
        final var command = ChangePlanCommand.with(
                stored.id().getValue(), "  ", NEW_DESCRIPTION, CURRENCY, NEW_PRICE);

        final var actual = assertThrows(DomainException.class, () -> useCase.execute(command));

        assertEquals("'name' should not be empty", actual.getMessage());
        verify(planGateway, never()).save(any());
    }

    private static ChangePlanCommand commandFor(final Plan plan) {
        return commandFor(plan.id());
    }

    private static ChangePlanCommand commandFor(final PlanId planId) {
        return ChangePlanCommand.with(planId.getValue(), NEW_NAME, NEW_DESCRIPTION, CURRENCY, NEW_PRICE);
    }
}
