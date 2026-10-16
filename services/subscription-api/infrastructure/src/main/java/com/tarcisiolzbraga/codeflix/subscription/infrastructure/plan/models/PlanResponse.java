package com.tarcisiolzbraga.codeflix.subscription.infrastructure.plan.models;

import com.tarcisiolzbraga.codeflix.subscription.application.plan.PlanOutput;
import java.math.BigDecimal;
import java.time.Instant;

// O plano como o corpo da resposta o mostra. Convertido do output por from(...): nenhum tipo da
// application aparece no JSON nem no schema OpenAPI, então mudar a saída de um caso de uso não
// quebra o contrato da API.
public record PlanResponse(
        String id,
        String name,
        String description,
        boolean active,
        String currency,
        BigDecimal price,
        Instant createdAt,
        Instant updatedAt) {

    public static PlanResponse from(final PlanOutput output) {
        return new PlanResponse(
                output.id(),
                output.name(),
                output.description(),
                output.active(),
                output.currency(),
                output.price(),
                output.createdAt(),
                output.updatedAt());
    }
}
