package com.tarcisiolzbraga.codeflix.subscription.domain.plan;

import com.tarcisiolzbraga.codeflix.subscription.domain.pagination.PageRequest;
import com.tarcisiolzbraga.codeflix.subscription.domain.pagination.Pagination;
import java.util.Optional;

public interface PlanGateway {

    Optional<Plan> planOfId(PlanId anId);

    // Devolve o próprio agregado recebido. A versão dele continua sendo a que foi usada no lock, e
    // não a que ficou gravada: para salvar de novo é preciso ler o plano outra vez.
    Plan save(Plan aPlan);

    // Duas consultas, e não uma com booleano de filtro: booleano escolhendo entre dois
    // comportamentos é proibido no monorepo, e aqui os dois têm público diferente — o catálogo
    // aberto mostra o que está à venda, e o operador precisa ver o que tirou de venda.
    Pagination<Plan> activePlansOfPage(PageRequest aPage);

    Pagination<Plan> plansOfPage(PageRequest aPage);
}
