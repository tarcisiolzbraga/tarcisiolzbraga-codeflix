package com.tarcisiolzbraga.codeflix.subscription.application.plan.list;

import com.tarcisiolzbraga.codeflix.subscription.domain.pagination.PageRequest;
import com.tarcisiolzbraga.codeflix.subscription.domain.pagination.Pagination;
import com.tarcisiolzbraga.codeflix.subscription.domain.plan.PlanGateway;
import java.util.Objects;

public class DefaultListPlansUseCase extends ListPlansUseCase {

    private final PlanGateway planGateway;

    public DefaultListPlansUseCase(final PlanGateway planGateway) {
        this.planGateway = Objects.requireNonNull(planGateway, "'planGateway' should not be null");
    }

    @Override
    public Pagination<PlanListOutput> execute(final PageRequest aPage) {
        Objects.requireNonNull(aPage, "'aPage' should not be null");
        return this.planGateway.plansOfPage(aPage).map(PlanListOutput::from);
    }
}
