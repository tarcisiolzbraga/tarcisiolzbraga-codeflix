package com.tarcisiolzbraga.codeflix.subscription.infrastructure.plan.models;

import com.tarcisiolzbraga.codeflix.subscription.application.plan.list.PlanListOutput;
import java.math.BigDecimal;

// O plano como a listagem do operador o mostra. Traz `active` porque aqui ele varia: é esta rota que
// mostra o que foi tirado de venda, que é o que o operador precisa ver para pôr de volta.
public record PlanListResponse(
        String id, String name, String description, String currency, BigDecimal price, boolean active) {

    public static PlanListResponse from(final PlanListOutput output) {
        return new PlanListResponse(
                output.id(),
                output.name(),
                output.description(),
                output.currency(),
                output.price(),
                output.active());
    }
}
