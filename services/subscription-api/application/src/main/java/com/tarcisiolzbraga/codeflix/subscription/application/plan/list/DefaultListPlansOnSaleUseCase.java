package com.tarcisiolzbraga.codeflix.subscription.application.plan.list;

import com.tarcisiolzbraga.codeflix.subscription.domain.pagination.PageRequest;
import com.tarcisiolzbraga.codeflix.subscription.domain.pagination.Pagination;
import com.tarcisiolzbraga.codeflix.subscription.domain.plan.PlanGateway;
import java.util.Objects;

public class DefaultListPlansOnSaleUseCase extends ListPlansOnSaleUseCase {

    private final PlanGateway planGateway;

    public DefaultListPlansOnSaleUseCase(final PlanGateway planGateway) {
        this.planGateway = Objects.requireNonNull(planGateway, "'planGateway' should not be null");
    }

    @Override
    public Pagination<PlanListOutput> execute(final PageRequest aPage) {
        Objects.requireNonNull(aPage, "'aPage' should not be null");
        return this.planGateway.activePlansOfPage(aPage).map(PlanListOutput::from);
    }
}
