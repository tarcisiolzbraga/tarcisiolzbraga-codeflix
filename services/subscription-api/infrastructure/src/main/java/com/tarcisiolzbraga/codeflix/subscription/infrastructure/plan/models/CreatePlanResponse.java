package com.tarcisiolzbraga.codeflix.subscription.infrastructure.plan.models;

import com.tarcisiolzbraga.codeflix.subscription.application.plan.create.CreatePlanOutput;

public record CreatePlanResponse(String id) {

    public static CreatePlanResponse from(final CreatePlanOutput output) {
        return new CreatePlanResponse(output.id());
    }
}
