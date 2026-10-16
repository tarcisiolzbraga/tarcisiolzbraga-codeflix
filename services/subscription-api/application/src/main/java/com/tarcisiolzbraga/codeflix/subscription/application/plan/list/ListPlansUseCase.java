package com.tarcisiolzbraga.codeflix.subscription.application.plan.list;

import com.tarcisiolzbraga.codeflix.subscription.application.UseCase;
import com.tarcisiolzbraga.codeflix.subscription.domain.pagination.PageRequest;
import com.tarcisiolzbraga.codeflix.subscription.domain.pagination.Pagination;

// A listagem de quem administra: traz também o que está fora de venda, que é justamente o que ele
// precisa ver para pôr de volta.
public abstract class ListPlansUseCase extends UseCase<PageRequest, Pagination<PlanListOutput>> {}
