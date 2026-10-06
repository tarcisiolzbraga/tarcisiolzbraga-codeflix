package com.tarcisiolzbraga.codeflix.videos.infrastructure.configuration;

import com.tarcisiolzbraga.codeflix.videos.application.castmember.delete.DefaultDeleteCastMemberUseCase;
import com.tarcisiolzbraga.codeflix.videos.application.castmember.delete.DeleteCastMemberUseCase;
import com.tarcisiolzbraga.codeflix.videos.application.castmember.get.DefaultGetCastMembersByIdUseCase;
import com.tarcisiolzbraga.codeflix.videos.application.castmember.get.GetCastMembersByIdUseCase;
import com.tarcisiolzbraga.codeflix.videos.application.castmember.list.DefaultListCastMembersUseCase;
import com.tarcisiolzbraga.codeflix.videos.application.castmember.list.ListCastMembersUseCase;
import com.tarcisiolzbraga.codeflix.videos.application.castmember.save.DefaultSaveCastMemberUseCase;
import com.tarcisiolzbraga.codeflix.videos.application.castmember.save.SaveCastMemberUseCase;
import com.tarcisiolzbraga.codeflix.videos.domain.castmember.CastMemberGateway;
import java.util.Objects;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CastMemberUseCaseConfig {

    private final CastMemberGateway castMemberGateway;

    public CastMemberUseCaseConfig(final CastMemberGateway castMemberGateway) {
        this.castMemberGateway =
                Objects.requireNonNull(castMemberGateway, "'castMemberGateway' should not be null");
    }

    @Bean
    SaveCastMemberUseCase saveCastMemberUseCase() {
        return new DefaultSaveCastMemberUseCase(this.castMemberGateway);
    }

    @Bean
    DeleteCastMemberUseCase deleteCastMemberUseCase() {
        return new DefaultDeleteCastMemberUseCase(this.castMemberGateway);
    }

    @Bean
    ListCastMembersUseCase listCastMembersUseCase() {
        return new DefaultListCastMembersUseCase(this.castMemberGateway);
    }

    @Bean
    GetCastMembersByIdUseCase getCastMembersByIdUseCase() {
        return new DefaultGetCastMembersByIdUseCase(this.castMemberGateway);
    }
}
