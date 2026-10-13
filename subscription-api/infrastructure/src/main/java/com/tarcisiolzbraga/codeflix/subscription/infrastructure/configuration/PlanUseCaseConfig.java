package com.tarcisiolzbraga.codeflix.subscription.infrastructure.configuration;

import com.tarcisiolzbraga.codeflix.subscription.application.plan.activate.ActivatePlanUseCase;
import com.tarcisiolzbraga.codeflix.subscription.application.plan.activate.DefaultActivatePlanUseCase;
import com.tarcisiolzbraga.codeflix.subscription.application.plan.change.ChangePlanUseCase;
import com.tarcisiolzbraga.codeflix.subscription.application.plan.change.DefaultChangePlanUseCase;
import com.tarcisiolzbraga.codeflix.subscription.application.plan.create.CreatePlanUseCase;
import com.tarcisiolzbraga.codeflix.subscription.application.plan.create.DefaultCreatePlanUseCase;
import com.tarcisiolzbraga.codeflix.subscription.application.plan.deactivate.DeactivatePlanUseCase;
import com.tarcisiolzbraga.codeflix.subscription.application.plan.deactivate.DefaultDeactivatePlanUseCase;
import com.tarcisiolzbraga.codeflix.subscription.application.plan.get.DefaultGetPlanByIdUseCase;
import com.tarcisiolzbraga.codeflix.subscription.application.plan.get.GetPlanByIdUseCase;
import com.tarcisiolzbraga.codeflix.subscription.domain.plan.PlanGateway;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Os casos de uso viram bean aqui, um @Bean cada: as classes de application e domain não levam
// anotação de framework, então é por aqui que elas entram no contexto.
@Configuration(proxyBeanMethods = false)
public class PlanUseCaseConfig {

    @Bean
    CreatePlanUseCase createPlanUseCase(final PlanGateway planGateway) {
        return new DefaultCreatePlanUseCase(planGateway);
    }

    @Bean
    GetPlanByIdUseCase getPlanByIdUseCase(final PlanGateway planGateway) {
        return new DefaultGetPlanByIdUseCase(planGateway);
    }

    @Bean
    ChangePlanUseCase changePlanUseCase(final PlanGateway planGateway) {
        return new DefaultChangePlanUseCase(planGateway);
    }

    @Bean
    ActivatePlanUseCase activatePlanUseCase(final PlanGateway planGateway) {
        return new DefaultActivatePlanUseCase(planGateway);
    }

    @Bean
    DeactivatePlanUseCase deactivatePlanUseCase(final PlanGateway planGateway) {
        return new DefaultDeactivatePlanUseCase(planGateway);
    }
}
