package com.tarcisiolzbraga.codeflix.subscription.application.plan.get;

import com.tarcisiolzbraga.codeflix.subscription.application.plan.PlanOutput;
import com.tarcisiolzbraga.codeflix.subscription.domain.exceptions.NotFoundException;
import com.tarcisiolzbraga.codeflix.subscription.domain.plan.Plan;
import com.tarcisiolzbraga.codeflix.subscription.domain.plan.PlanGateway;
import com.tarcisiolzbraga.codeflix.subscription.domain.plan.PlanId;
import java.util.Objects;

public class DefaultGetPlanByIdUseCase extends GetPlanByIdUseCase {

    private final PlanGateway planGateway;

    public DefaultGetPlanByIdUseCase(final PlanGateway planGateway) {
        this.planGateway = Objects.requireNonNull(planGateway, "'planGateway' should not be null");
    }

    // Devolve o plano inativo também, de propósito: aqui o `active` é estado de negócio que o
    // operador administra, e esconder o inativo tiraria o caminho de encontrar um para reativar. É o
    // mesmo raciocínio da admin-api, e o oposto do da videos-api, onde inativo não é servido.
    @Override
    public PlanOutput execute(final String planId) {
        final var anId = PlanId.from(planId);
        return this.planGateway
                .planOfId(anId)
                .map(PlanOutput::from)
                .orElseThrow(() -> NotFoundException.with(Plan.class, anId));
    }
}
