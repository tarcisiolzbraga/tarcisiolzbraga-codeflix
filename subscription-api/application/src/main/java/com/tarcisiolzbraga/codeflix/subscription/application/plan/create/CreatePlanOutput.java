package com.tarcisiolzbraga.codeflix.subscription.application.plan.create;

import com.tarcisiolzbraga.codeflix.subscription.domain.plan.Plan;

public record CreatePlanOutput(String id) {

    public static CreatePlanOutput from(final Plan aPlan) {
        return new CreatePlanOutput(aPlan.id().getValue());
    }
}
