package com.tarcisiolzbraga.codeflix.subscription.infrastructure.plan.models;

import com.tarcisiolzbraga.codeflix.subscription.application.plan.change.ChangePlanOutput;

public record ChangePlanResponse(String id) {

    public static ChangePlanResponse from(final ChangePlanOutput output) {
        return new ChangePlanResponse(output.id());
    }
}
