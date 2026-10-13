package com.tarcisiolzbraga.codeflix.subscription.domain.plan;

import com.tarcisiolzbraga.codeflix.subscription.domain.money.Money;

// Um comando por regra, sem flag: o ChangePlan mexe só nos dados e não decide ativação. Na
// referência ele carrega um booleano `active` que escolhe entre ativar e desativar, e os dois
// CLAUDE.md do monorepo proíbem exatamente isso.
public sealed interface PlanCommand {

    record ChangePlan(String name, String description, Money price) implements PlanCommand {}

    record ActivatePlan() implements PlanCommand {}

    record DeactivatePlan() implements PlanCommand {}
}
