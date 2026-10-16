package com.tarcisiolzbraga.codeflix.videos.domain.castmember;

import com.tarcisiolzbraga.codeflix.videos.domain.pagination.Pagination;
import com.tarcisiolzbraga.codeflix.videos.domain.pagination.SearchQuery;
import java.util.List;
import java.util.Optional;
import java.util.Set;

// O save está aqui pelo mesmo motivo do CategoryGateway: este lado não cria membro de elenco, ele
// guarda no próprio armazenamento a versão que o admin-codeflix publicou.
public interface CastMemberGateway {

    CastMember save(CastMember castMember);

    void deleteById(CastMemberID id);

    Optional<CastMember> findById(CastMemberID id);

    // Devolve List, e não Pagination, porque o resultado é limitado pelos ids recebidos: é resolver
    // referências que o chamador já tem em mão, não listagem.
    List<CastMember> findAllById(Set<CastMemberID> ids);

    Pagination<CastMember> findAll(SearchQuery query);
}
