package com.tarcisiolzbraga.codeflix.subscription.domain.plan;

import com.tarcisiolzbraga.codeflix.subscription.domain.money.Money;
import com.tarcisiolzbraga.codeflix.subscription.domain.utils.InstantUtils;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

// Dados de teste do plano, iguais em todo teste que não se importa com os valores. Sem sorteio: o
// teste que falha falha sempre, e o bloco given continua dizendo o que importa para aquele caso.
public final class PlanFixture {

    public static final String NAME = "Plus";
    public static final String DESCRIPTION = "O catálogo inteiro, em até quatro telas";
    public static final String CURRENCY = "BRL";
    public static final BigDecimal AMOUNT = new BigDecimal("49.90");

    private PlanFixture() {}

    public static Money price() {
        return Money.of(CURRENCY, AMOUNT);
    }

    public static Plan plan() {
        return Plan.newPlan(PlanId.unique(), NAME, DESCRIPTION, true, price());
    }

    public static Plan inactivePlan() {
        return Plan.newPlan(PlanId.unique(), NAME, DESCRIPTION, false, price());
    }

    // Um plano como ele volta do banco: com versão e com datas no passado, para o teste conseguir
    // afirmar que o updatedAt andou.
    public static Plan storedPlan() {
        return storedPlan(1);
    }

    public static Plan storedPlan(final int version) {
        final var longAgo = InstantUtils.now().minus(30, ChronoUnit.DAYS);
        return Plan.with(PlanId.unique(), version, NAME, DESCRIPTION, true, price(), longAgo, longAgo);
    }

    public static Instant longAgo() {
        return InstantUtils.now().minus(30, ChronoUnit.DAYS);
    }
}
