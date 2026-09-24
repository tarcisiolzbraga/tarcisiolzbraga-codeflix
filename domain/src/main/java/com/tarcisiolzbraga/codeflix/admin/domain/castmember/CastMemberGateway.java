package com.tarcisiolzbraga.codeflix.admin.domain.castmember;

import com.tarcisiolzbraga.codeflix.admin.domain.pagination.Pagination;
import com.tarcisiolzbraga.codeflix.admin.domain.pagination.SearchQuery;
import java.util.Optional;

public interface CastMemberGateway {

    CastMember create(CastMember castMember);

    CastMember update(CastMember castMember);

    void deleteById(CastMemberID id);

    Optional<CastMember> findById(CastMemberID id);

    Pagination<CastMember> findAll(SearchQuery query);
}
