package com.tarcisiolzbraga.codeflix.subscription.application.plan.activate;

import com.tarcisiolzbraga.codeflix.subscription.application.plan.PlanOutput;
import com.tarcisiolzbraga.codeflix.subscription.domain.exceptions.NotFoundException;
import com.tarcisiolzbraga.codeflix.subscription.domain.plan.Plan;
import com.tarcisiolzbraga.codeflix.subscription.domain.plan.PlanCommand;
import com.tarcisiolzbraga.codeflix.subscription.domain.plan.PlanGateway;
import com.tarcisiolzbraga.codeflix.subscription.domain.plan.PlanId;
import java.util.Objects;

public class DefaultActivatePlanUseCase extends ActivatePlanUseCase {

    private final PlanGateway planGateway;

    public DefaultActivatePlanUseCase(final PlanGateway planGateway) {
        this.planGateway = Objects.requireNonNull(planGateway, "'planGateway' should not be null");
    }

    // Plano que não existe deixa a NotFoundException subir: não há entrada para validar, então um
    // retorno de erro aqui seria caminho morto.
    @Override
    public PlanOutput execute(final String planId) {
        final var anId = PlanId.from(planId);
        final var aPlan = this.planGateway
                .planOfId(anId)
                .orElseThrow(() -> NotFoundException.with(Plan.class, anId));

        aPlan.execute(new PlanCommand.ActivatePlan());

        return PlanOutput.from(this.planGateway.save(aPlan));
    }
}
