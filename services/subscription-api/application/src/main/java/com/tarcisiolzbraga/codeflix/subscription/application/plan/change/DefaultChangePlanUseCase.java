package com.tarcisiolzbraga.codeflix.subscription.application.plan.change;

import com.tarcisiolzbraga.codeflix.subscription.domain.exceptions.NotFoundException;
import com.tarcisiolzbraga.codeflix.subscription.domain.money.Money;
import com.tarcisiolzbraga.codeflix.subscription.domain.plan.Plan;
import com.tarcisiolzbraga.codeflix.subscription.domain.plan.PlanCommand;
import com.tarcisiolzbraga.codeflix.subscription.domain.plan.PlanGateway;
import com.tarcisiolzbraga.codeflix.subscription.domain.plan.PlanId;
import java.util.Objects;

public class DefaultChangePlanUseCase extends ChangePlanUseCase {

    private final PlanGateway planGateway;

    public DefaultChangePlanUseCase(final PlanGateway planGateway) {
        this.planGateway = Objects.requireNonNull(planGateway, "'planGateway' should not be null");
    }

    @Override
    public ChangePlanOutput execute(final ChangePlanCommand command) {
        Objects.requireNonNull(command, "'command' should not be null");
        final var anId = PlanId.from(command.planId());
        final var aPlan = this.planGateway
                .planOfId(anId)
                .orElseThrow(() -> NotFoundException.with(Plan.class, anId));

        aPlan.execute(new PlanCommand.ChangePlan(
                command.name(), command.description(), Money.of(command.currency(), command.price())));

        return ChangePlanOutput.from(this.planGateway.save(aPlan));
    }
}
