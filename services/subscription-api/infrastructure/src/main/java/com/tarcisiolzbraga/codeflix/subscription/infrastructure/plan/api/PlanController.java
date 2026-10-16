package com.tarcisiolzbraga.codeflix.subscription.infrastructure.plan.api;

import com.tarcisiolzbraga.codeflix.subscription.application.plan.activate.ActivatePlanUseCase;
import com.tarcisiolzbraga.codeflix.subscription.application.plan.change.ChangePlanCommand;
import com.tarcisiolzbraga.codeflix.subscription.application.plan.change.ChangePlanUseCase;
import com.tarcisiolzbraga.codeflix.subscription.application.plan.create.CreatePlanCommand;
import com.tarcisiolzbraga.codeflix.subscription.application.plan.create.CreatePlanUseCase;
import com.tarcisiolzbraga.codeflix.subscription.application.plan.list.ListPlansOnSaleUseCase;
import com.tarcisiolzbraga.codeflix.subscription.application.plan.list.ListPlansUseCase;
import com.tarcisiolzbraga.codeflix.subscription.domain.pagination.Pagination;
import com.tarcisiolzbraga.codeflix.subscription.infrastructure.api.PageQuery;
import com.tarcisiolzbraga.codeflix.subscription.infrastructure.plan.models.PlanListResponse;
import com.tarcisiolzbraga.codeflix.subscription.infrastructure.plan.models.PlanOnSaleResponse;
import com.tarcisiolzbraga.codeflix.subscription.application.plan.deactivate.DeactivatePlanUseCase;
import com.tarcisiolzbraga.codeflix.subscription.application.plan.get.GetPlanByIdUseCase;
import com.tarcisiolzbraga.codeflix.subscription.infrastructure.plan.models.ChangePlanRequest;
import com.tarcisiolzbraga.codeflix.subscription.infrastructure.plan.models.ChangePlanResponse;
import com.tarcisiolzbraga.codeflix.subscription.infrastructure.plan.models.CreatePlanRequest;
import com.tarcisiolzbraga.codeflix.subscription.infrastructure.plan.models.CreatePlanResponse;
import com.tarcisiolzbraga.codeflix.subscription.infrastructure.plan.models.PlanResponse;
import java.net.URI;
import java.util.Objects;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

// Controller fino: lê a entrada, chama o caso de uso e mapeia a resposta. Quem recusa dado inválido
// é o agregado, e quem traduz a recusa em status é o GlobalExceptionHandler.
@RestController
public class PlanController implements PlanAPI {

    private final CreatePlanUseCase createPlanUseCase;
    private final ListPlansOnSaleUseCase listPlansOnSaleUseCase;
    private final ListPlansUseCase listPlansUseCase;
    private final ChangePlanUseCase changePlanUseCase;
    private final ActivatePlanUseCase activatePlanUseCase;
    private final DeactivatePlanUseCase deactivatePlanUseCase;
    private final GetPlanByIdUseCase getPlanByIdUseCase;

    public PlanController(
            final CreatePlanUseCase createPlanUseCase,
            final ListPlansOnSaleUseCase listPlansOnSaleUseCase,
            final ListPlansUseCase listPlansUseCase,
            final ChangePlanUseCase changePlanUseCase,
            final ActivatePlanUseCase activatePlanUseCase,
            final DeactivatePlanUseCase deactivatePlanUseCase,
            final GetPlanByIdUseCase getPlanByIdUseCase) {
        this.createPlanUseCase = Objects.requireNonNull(createPlanUseCase);
        this.listPlansOnSaleUseCase = Objects.requireNonNull(listPlansOnSaleUseCase);
        this.listPlansUseCase = Objects.requireNonNull(listPlansUseCase);
        this.changePlanUseCase = Objects.requireNonNull(changePlanUseCase);
        this.activatePlanUseCase = Objects.requireNonNull(activatePlanUseCase);
        this.deactivatePlanUseCase = Objects.requireNonNull(deactivatePlanUseCase);
        this.getPlanByIdUseCase = Objects.requireNonNull(getPlanByIdUseCase);
    }

    @Override
    public ResponseEntity<CreatePlanResponse> create(final CreatePlanRequest request) {
        final var command = CreatePlanCommand.with(
                request.name(), request.description(), request.isActive(), request.currency(), request.price());
        final var response = CreatePlanResponse.from(this.createPlanUseCase.execute(command));
        return ResponseEntity.created(URI.create("/plans/" + response.id())).body(response);
    }

    @Override
    public Pagination<PlanOnSaleResponse> listOnSale(final PageQuery query) {
        return this.listPlansOnSaleUseCase.execute(query.toPageRequest()).map(PlanOnSaleResponse::from);
    }

    @Override
    public Pagination<PlanListResponse> list(final PageQuery query) {
        return this.listPlansUseCase.execute(query.toPageRequest()).map(PlanListResponse::from);
    }

    @Override
    public PlanResponse getById(final String id) {
        return PlanResponse.from(this.getPlanByIdUseCase.execute(id));
    }

    @Override
    public ChangePlanResponse change(final String id, final ChangePlanRequest request) {
        final var command = ChangePlanCommand.with(
                id, request.name(), request.description(), request.currency(), request.price());
        return ChangePlanResponse.from(this.changePlanUseCase.execute(command));
    }

    @Override
    public PlanResponse activate(final String id) {
        return PlanResponse.from(this.activatePlanUseCase.execute(id));
    }

    @Override
    public PlanResponse deactivate(final String id) {
        return PlanResponse.from(this.deactivatePlanUseCase.execute(id));
    }
}
