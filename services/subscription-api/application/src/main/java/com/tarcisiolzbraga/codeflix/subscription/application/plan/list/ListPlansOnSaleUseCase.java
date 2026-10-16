package com.tarcisiolzbraga.codeflix.subscription.application.plan.list;

import com.tarcisiolzbraga.codeflix.subscription.application.UseCase;
import com.tarcisiolzbraga.codeflix.subscription.domain.pagination.PageRequest;
import com.tarcisiolzbraga.codeflix.subscription.domain.pagination.Pagination;

// O catálogo de quem vai escolher um plano: só os que estão à venda.
public abstract class ListPlansOnSaleUseCase extends UseCase<PageRequest, Pagination<PlanListOutput>> {}
