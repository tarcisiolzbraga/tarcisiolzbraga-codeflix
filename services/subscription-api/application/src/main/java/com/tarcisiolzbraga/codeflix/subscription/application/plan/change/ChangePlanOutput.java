package com.tarcisiolzbraga.codeflix.subscription.application.plan.change;

import com.tarcisiolzbraga.codeflix.subscription.domain.plan.Plan;

public record ChangePlanOutput(String id) {

    public static ChangePlanOutput from(final Plan aPlan) {
        return new ChangePlanOutput(aPlan.id().getValue());
    }
}
