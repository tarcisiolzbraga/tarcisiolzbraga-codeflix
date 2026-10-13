package com.tarcisiolzbraga.codeflix.subscription.infrastructure.plan.models;

import java.math.BigDecimal;

public record CreatePlanRequest(
        String name, String description, Boolean active, String currency, BigDecimal price) {

    // Ausente no JSON significa inativo, ao contrário das categorias da admin-api, que nascem
    // ativas: aqui um plano ativo é um plano à venda, e pôr algo à venda é ato deliberado. Quem
    // quer o plano no ar manda active, ou chama a rota de ativar depois.
    public boolean isActive() {
        return Boolean.TRUE.equals(this.active);
    }
}
