package com.tarcisiolzbraga.codeflix.subscription.application.plan.change;

import java.math.BigDecimal;

// Sem o campo `active`: mudar dados não decide ativação, que tem caso de uso e rota próprios.
public record ChangePlanCommand(
        String planId, String name, String description, String currency, BigDecimal price) {

    public static ChangePlanCommand with(
            final String planId,
            final String name,
            final String description,
            final String currency,
            final BigDecimal price) {
        return new ChangePlanCommand(planId, name, description, currency, price);
    }
}
