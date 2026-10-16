package com.tarcisiolzbraga.codeflix.subscription.infrastructure.plan.models;

import com.tarcisiolzbraga.codeflix.subscription.application.plan.list.PlanListOutput;
import java.math.BigDecimal;

// O plano como o catálogo aberto o mostra: o bastante para uma tela de escolha.
//
// **Sem `active`**, diferente da listagem do operador: nesta rota todos estão à venda, então o campo
// seria sempre `true` — e campo que nunca varia é ruído que o cliente aprende a ignorar.
public record PlanOnSaleResponse(
        String id, String name, String description, String currency, BigDecimal price) {

    public static PlanOnSaleResponse from(final PlanListOutput output) {
        return new PlanOnSaleResponse(
                output.id(), output.name(), output.description(), output.currency(), output.price());
    }
}
