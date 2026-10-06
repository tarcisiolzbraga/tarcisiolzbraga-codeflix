package com.tarcisiolzbraga.codeflix.admin.domain.castmember;

import com.tarcisiolzbraga.codeflix.admin.domain.pagination.Pagination;
import com.tarcisiolzbraga.codeflix.admin.domain.pagination.SearchQuery;
import java.util.Optional;
import java.util.Set;

public interface CastMemberGateway {

    CastMember create(CastMember castMember);

    CastMember update(CastMember castMember);

    void deleteById(CastMemberID id);

    Optional<CastMember> findById(CastMemberID id);

    Pagination<CastMember> findAll(SearchQuery query);

    // Resultado limitado pelos IDs recebidos, então não é listagem.
    Set<CastMemberID> findExistingIds(Set<CastMemberID> ids);
}
