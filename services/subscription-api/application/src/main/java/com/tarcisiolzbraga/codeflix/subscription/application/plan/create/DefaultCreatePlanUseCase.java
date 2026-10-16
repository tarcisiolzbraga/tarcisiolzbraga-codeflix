package com.tarcisiolzbraga.codeflix.subscription.application.plan.create;

import com.tarcisiolzbraga.codeflix.subscription.domain.money.Money;
import com.tarcisiolzbraga.codeflix.subscription.domain.plan.Plan;
import com.tarcisiolzbraga.codeflix.subscription.domain.plan.PlanGateway;
import com.tarcisiolzbraga.codeflix.subscription.domain.plan.PlanId;
import java.util.Objects;

public class DefaultCreatePlanUseCase extends CreatePlanUseCase {

    private final PlanGateway planGateway;

    public DefaultCreatePlanUseCase(final PlanGateway planGateway) {
        this.planGateway = Objects.requireNonNull(planGateway, "'planGateway' should not be null");
    }

    // O agregado recusa o que for inválido ao ser montado, com DomainException, então o gateway só é
    // chamado com um plano bem formado.
    @Override
    public CreatePlanOutput execute(final CreatePlanCommand command) {
        Objects.requireNonNull(command, "'command' should not be null");
        final var aPlan = Plan.newPlan(
                PlanId.unique(),
                command.name(),
                command.description(),
                command.active(),
                Money.of(command.currency(), command.price()));
        return CreatePlanOutput.from(this.planGateway.save(aPlan));
    }
}
