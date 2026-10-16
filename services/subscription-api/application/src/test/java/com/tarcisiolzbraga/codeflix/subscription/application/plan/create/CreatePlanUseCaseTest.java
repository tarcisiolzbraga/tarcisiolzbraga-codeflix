package com.tarcisiolzbraga.codeflix.subscription.application.plan.create;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tarcisiolzbraga.codeflix.subscription.domain.exceptions.DomainException;
import com.tarcisiolzbraga.codeflix.subscription.domain.plan.Plan;
import com.tarcisiolzbraga.codeflix.subscription.domain.plan.PlanFixture;
import com.tarcisiolzbraga.codeflix.subscription.domain.plan.PlanGateway;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CreatePlanUseCaseTest {

    private static final String NAME = PlanFixture.NAME;
    private static final String DESCRIPTION = PlanFixture.DESCRIPTION;
    private static final String CURRENCY = PlanFixture.CURRENCY;
    private static final BigDecimal PRICE = PlanFixture.AMOUNT;

    @Mock
    private PlanGateway planGateway;

    @InjectMocks
    private DefaultCreatePlanUseCase useCase;

    @Captor
    private ArgumentCaptor<Plan> savedPlan;

    @Test
    void givenValidCommand_whenExecutes_thenReturnsTheIdOfTheSavedPlan() {
        final var command = CreatePlanCommand.with(NAME, DESCRIPTION, true, CURRENCY, PRICE);
        when(planGateway.save(any())).thenAnswer(returnsFirstArg());

        final var actual = useCase.execute(command);

        assertNotNull(actual.id());
    }

    @Test
    void givenValidCommand_whenExecutes_thenSavesAPlanWithTheGivenValues() {
        final var command = CreatePlanCommand.with(NAME, DESCRIPTION, true, CURRENCY, PRICE);
        when(planGateway.save(any())).thenAnswer(returnsFirstArg());

        useCase.execute(command);

        verify(planGateway).save(savedPlan.capture());
        assertEquals(NAME, savedPlan.getValue().name());
    }

    @Test
    void givenValidCommand_whenExecutes_thenSavesThePriceWithTheGivenCurrency() {
        final var command = CreatePlanCommand.with(NAME, DESCRIPTION, true, CURRENCY, PRICE);
        when(planGateway.save(any())).thenAnswer(returnsFirstArg());

        useCase.execute(command);

        verify(planGateway).save(savedPlan.capture());
        assertEquals(PlanFixture.price(), savedPlan.getValue().price());
    }

    @Test
    void givenAnInactiveCommand_whenExecutes_thenSavesAnInactivePlan() {
        final var command = CreatePlanCommand.with(NAME, DESCRIPTION, false, CURRENCY, PRICE);
        when(planGateway.save(any())).thenAnswer(returnsFirstArg());

        useCase.execute(command);

        verify(planGateway).save(savedPlan.capture());
        assertFalse(savedPlan.getValue().active());
    }

    @Test
    void givenValidCommand_whenExecutes_thenTheSavedPlanStartsAtVersionZero() {
        final var command = CreatePlanCommand.with(NAME, DESCRIPTION, true, CURRENCY, PRICE);
        when(planGateway.save(any())).thenAnswer(returnsFirstArg());

        useCase.execute(command);

        verify(planGateway).save(savedPlan.capture());
        assertEquals(0, savedPlan.getValue().version());
    }

    @Test
    void givenABlankName_whenExecutes_thenThrowsAndNeverTouchesTheGateway() {
        final var command = CreatePlanCommand.with("  ", DESCRIPTION, true, CURRENCY, PRICE);

        final var actual = assertThrows(DomainException.class, () -> useCase.execute(command));

        assertEquals("'name' should not be empty", actual.getMessage());
        verify(planGateway, never()).save(any());
    }

    @Test
    void givenANegativePrice_whenExecutes_thenThrowsAndNeverTouchesTheGateway() {
        final var command = CreatePlanCommand.with(NAME, DESCRIPTION, true, CURRENCY, new BigDecimal("-1"));

        final var actual = assertThrows(DomainException.class, () -> useCase.execute(command));

        assertEquals("'amount' should not be negative", actual.getMessage());
        verify(planGateway, never()).save(any());
    }

    // Falha de infraestrutura não é erro de validação: tem de subir, para não virar 422.
    @Test
    void givenAGatewayThatFails_whenExecutes_thenTheFailureSurfaces() {
        final var command = CreatePlanCommand.with(NAME, DESCRIPTION, true, CURRENCY, PRICE);
        doThrow(new IllegalStateException("banco fora")).when(planGateway).save(any());

        final var actual = assertThrows(IllegalStateException.class, () -> useCase.execute(command));

        assertTrue(actual.getMessage().contains("banco fora"));
    }
}
