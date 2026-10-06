package com.tarcisiolzbraga.codeflix.videos.application.castmember.list;

import com.tarcisiolzbraga.codeflix.videos.application.castmember.CastMemberOutput;
import com.tarcisiolzbraga.codeflix.videos.domain.castmember.CastMemberGateway;
import com.tarcisiolzbraga.codeflix.videos.domain.pagination.Pagination;
import com.tarcisiolzbraga.codeflix.videos.domain.pagination.SearchQuery;
import java.util.Objects;

public class DefaultListCastMembersUseCase extends ListCastMembersUseCase {

    private final CastMemberGateway castMemberGateway;

    public DefaultListCastMembersUseCase(final CastMemberGateway castMemberGateway) {
        this.castMemberGateway =
                Objects.requireNonNull(castMemberGateway, "'castMemberGateway' should not be null");
    }

    @Override
    public Pagination<CastMemberOutput> execute(final SearchQuery input) {
        Objects.requireNonNull(input, "'input' should not be null");
        return this.castMemberGateway.findAll(input).map(CastMemberOutput::from);
    }
}
