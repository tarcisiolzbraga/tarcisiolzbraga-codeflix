package com.tarcisiolzbraga.codeflix.videos.infrastructure.graphql;

import com.tarcisiolzbraga.codeflix.videos.application.castmember.list.ListCastMembersUseCase;
import com.tarcisiolzbraga.codeflix.videos.infrastructure.castmember.models.GqlCastMemberPage;
import com.tarcisiolzbraga.codeflix.videos.infrastructure.castmember.models.GqlCastMemberQuery;
import java.util.Objects;
import com.tarcisiolzbraga.codeflix.videos.infrastructure.security.Roles;
import org.springframework.graphql.data.method.annotation.Arguments;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Controller;

@Controller
public class CastMemberGraphQLController {

    private final ListCastMembersUseCase listCastMembersUseCase;

    public CastMemberGraphQLController(final ListCastMembersUseCase listCastMembersUseCase) {
        this.listCastMembersUseCase =
                Objects.requireNonNull(listCastMembersUseCase, "'listCastMembersUseCase' should not be null");
    }

    @QueryMapping
    @Secured({Roles.SUBSCRIBER, Roles.ADMIN})
    public GqlCastMemberPage castMembers(@Arguments final GqlCastMemberQuery query) {
        return GqlCastMemberPage.from(this.listCastMembersUseCase.execute(query.toSearchQuery()));
    }
}
