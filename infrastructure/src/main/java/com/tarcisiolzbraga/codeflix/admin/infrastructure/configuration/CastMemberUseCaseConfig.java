package com.tarcisiolzbraga.codeflix.admin.infrastructure.configuration;

import com.tarcisiolzbraga.codeflix.admin.application.castmember.activate.ActivateCastMemberUseCase;
import com.tarcisiolzbraga.codeflix.admin.application.castmember.activate.DefaultActivateCastMemberUseCase;
import com.tarcisiolzbraga.codeflix.admin.application.castmember.create.CreateCastMemberUseCase;
import com.tarcisiolzbraga.codeflix.admin.application.castmember.create.DefaultCreateCastMemberUseCase;
import com.tarcisiolzbraga.codeflix.admin.application.castmember.deactivate.DeactivateCastMemberUseCase;
import com.tarcisiolzbraga.codeflix.admin.application.castmember.deactivate.DefaultDeactivateCastMemberUseCase;
import com.tarcisiolzbraga.codeflix.admin.application.castmember.delete.DefaultDeleteCastMemberUseCase;
import com.tarcisiolzbraga.codeflix.admin.application.castmember.delete.DeleteCastMemberUseCase;
import com.tarcisiolzbraga.codeflix.admin.application.castmember.get.DefaultGetCastMemberByIdUseCase;
import com.tarcisiolzbraga.codeflix.admin.application.castmember.get.GetCastMemberByIdUseCase;
import com.tarcisiolzbraga.codeflix.admin.application.castmember.list.DefaultListCastMembersUseCase;
import com.tarcisiolzbraga.codeflix.admin.application.castmember.list.ListCastMembersUseCase;
import com.tarcisiolzbraga.codeflix.admin.application.castmember.update.DefaultUpdateCastMemberUseCase;
import com.tarcisiolzbraga.codeflix.admin.application.castmember.update.UpdateCastMemberUseCase;
import com.tarcisiolzbraga.codeflix.admin.domain.castmember.CastMemberGateway;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// O component scan só enxerga a infrastructure, então os casos de uso são registrados aqui.
@Configuration
public class CastMemberUseCaseConfig {

    private final CastMemberGateway castMemberGateway;

    public CastMemberUseCaseConfig(final CastMemberGateway castMemberGateway) {
        this.castMemberGateway = castMemberGateway;
    }

    @Bean
    CreateCastMemberUseCase createCastMemberUseCase() {
        return new DefaultCreateCastMemberUseCase(this.castMemberGateway);
    }

    @Bean
    GetCastMemberByIdUseCase getCastMemberByIdUseCase() {
        return new DefaultGetCastMemberByIdUseCase(this.castMemberGateway);
    }

    @Bean
    ListCastMembersUseCase listCastMembersUseCase() {
        return new DefaultListCastMembersUseCase(this.castMemberGateway);
    }

    @Bean
    UpdateCastMemberUseCase updateCastMemberUseCase() {
        return new DefaultUpdateCastMemberUseCase(this.castMemberGateway);
    }

    @Bean
    ActivateCastMemberUseCase activateCastMemberUseCase() {
        return new DefaultActivateCastMemberUseCase(this.castMemberGateway);
    }

    @Bean
    DeactivateCastMemberUseCase deactivateCastMemberUseCase() {
        return new DefaultDeactivateCastMemberUseCase(this.castMemberGateway);
    }

    @Bean
    DeleteCastMemberUseCase deleteCastMemberUseCase() {
        return new DefaultDeleteCastMemberUseCase(this.castMemberGateway);
    }
}
