package com.tarcisiolzbraga.codeflix.subscription.domain.plan;

import java.util.Optional;

// Só o que algum caso de uso desta fatia usa. O existsPlanOfId e a listagem da referência entram
// quando a assinatura precisar deles, e não antes.
public interface PlanGateway {

    Optional<Plan> planOfId(PlanId anId);

    // Devolve o próprio agregado recebido. A versão dele continua sendo a que foi usada no lock, e
    // não a que ficou gravada: para salvar de novo é preciso ler o plano outra vez.
    Plan save(Plan aPlan);
}
