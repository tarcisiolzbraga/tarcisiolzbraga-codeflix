package com.tarcisiolzbraga.codeflix.subscription.application.plan.create;

import java.math.BigDecimal;

public record CreatePlanCommand(
        String name, String description, Boolean active, String currency, BigDecimal price) {

    public static CreatePlanCommand with(
            final String name,
            final String description,
            final Boolean active,
            final String currency,
            final BigDecimal price) {
        return new CreatePlanCommand(name, description, active, currency, price);
    }
}
