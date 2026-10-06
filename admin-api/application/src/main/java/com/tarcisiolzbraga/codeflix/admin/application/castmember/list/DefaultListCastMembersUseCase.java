package com.tarcisiolzbraga.codeflix.admin.application.castmember.list;

import com.tarcisiolzbraga.codeflix.admin.domain.castmember.CastMemberGateway;
import com.tarcisiolzbraga.codeflix.admin.domain.pagination.Pagination;
import com.tarcisiolzbraga.codeflix.admin.domain.pagination.SearchQuery;
import java.util.Objects;

public class DefaultListCastMembersUseCase extends ListCastMembersUseCase {

    private final CastMemberGateway castMemberGateway;

    public DefaultListCastMembersUseCase(final CastMemberGateway castMemberGateway) {
        this.castMemberGateway = Objects.requireNonNull(castMemberGateway, "'castMemberGateway' should not be null");
    }

    @Override
    public Pagination<CastMemberListOutput> execute(final SearchQuery input) {
        return this.castMemberGateway.findAll(input).map(CastMemberListOutput::from);
    }
}
