package com.tarcisiolzbraga.codeflix.subscription.application.plan;

import com.tarcisiolzbraga.codeflix.subscription.domain.plan.Plan;
import java.math.BigDecimal;
import java.time.Instant;

// O plano como a aplicação o entrega. O id sai como texto: a borda é o JSON, e é aqui que a
// conversão acontece — nenhum tipo do domínio atravessa a camada.
public record PlanOutput(
        String id,
        String name,
        String description,
        boolean active,
        String currency,
        BigDecimal price,
        Instant createdAt,
        Instant updatedAt) {

    public static PlanOutput from(final Plan aPlan) {
        return new PlanOutput(
                aPlan.id().getValue(),
                aPlan.name(),
                aPlan.description(),
                aPlan.active(),
                aPlan.price().currencyCode(),
                aPlan.price().amount(),
                aPlan.createdAt(),
                aPlan.updatedAt());
    }
}
