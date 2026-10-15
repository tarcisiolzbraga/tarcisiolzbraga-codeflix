package com.tarcisiolzbraga.codeflix.subscription.application.plan.list;

import com.tarcisiolzbraga.codeflix.subscription.domain.plan.Plan;
import java.math.BigDecimal;

// O plano como uma listagem o mostra: sem as datas, que não interessam a quem escolhe nem a quem
// administra uma lista. Quem quiser o resto busca pelo id.
public record PlanListOutput(
        String id, String name, String description, String currency, BigDecimal price, boolean active) {

    public static PlanListOutput from(final Plan aPlan) {
        return new PlanListOutput(
                aPlan.id().getValue(),
                aPlan.name(),
                aPlan.description(),
                aPlan.price().currencyCode(),
                aPlan.price().amount(),
                aPlan.active());
    }
}
