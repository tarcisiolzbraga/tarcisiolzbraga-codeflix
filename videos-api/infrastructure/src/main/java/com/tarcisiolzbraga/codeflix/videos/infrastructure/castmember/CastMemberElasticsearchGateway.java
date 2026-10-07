package com.tarcisiolzbraga.codeflix.videos.infrastructure.castmember;

import com.tarcisiolzbraga.codeflix.videos.domain.castmember.CastMember;
import com.tarcisiolzbraga.codeflix.videos.domain.castmember.CastMemberGateway;
import com.tarcisiolzbraga.codeflix.videos.domain.castmember.CastMemberID;
import com.tarcisiolzbraga.codeflix.videos.domain.pagination.Pagination;
import com.tarcisiolzbraga.codeflix.videos.domain.pagination.SearchQuery;
import com.tarcisiolzbraga.codeflix.videos.infrastructure.castmember.persistence.CastMemberDocument;
import com.tarcisiolzbraga.codeflix.videos.infrastructure.castmember.persistence.CastMemberRepository;
import com.tarcisiolzbraga.codeflix.videos.infrastructure.elasticsearch.SearchTerms;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.StreamSupport;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchOperations;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.stereotype.Component;

@Component
public class CastMemberElasticsearchGateway implements CastMemberGateway {

    private static final String NAME = "name";
    private static final String ACTIVE = "active";
    private static final String KEYWORD_SUFFIX = ".keyword";

    private final CastMemberRepository castMemberRepository;
    private final SearchOperations searchOperations;

    public CastMemberElasticsearchGateway(
            final CastMemberRepository castMemberRepository, final SearchOperations searchOperations) {
        this.castMemberRepository =
                Objects.requireNonNull(castMemberRepository, "'castMemberRepository' should not be null");
        this.searchOperations = Objects.requireNonNull(searchOperations, "'searchOperations' should not be null");
    }

    @Override
    public CastMember save(final CastMember castMember) {
        this.castMemberRepository.save(CastMemberDocument.from(castMember));
        return castMember;
    }

    @Override
    public void deleteById(final CastMemberID id) {
        this.castMemberRepository.deleteById(id.value());
    }

    // Vazio para o inativo, como no gateway da categoria: nada inativo é devolvido em leitura alguma.
    @Override
    public Optional<CastMember> findById(final CastMemberID id) {
        return this.castMemberRepository
                .findById(id.value())
                .filter(CastMemberDocument::isActive)
                .map(CastMemberDocument::toCastMember);
    }

    @Override
    public List<CastMember> findAllById(final Set<CastMemberID> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        final var values = ids.stream().map(CastMemberID::value).toList();
        // Resolução de relação: um vídeo ativo com membros de elenco inativos vem sem eles.
        return StreamSupport.stream(this.castMemberRepository.findAllById(values).spliterator(), false)
                .filter(CastMemberDocument::isActive)
                .map(CastMemberDocument::toCastMember)
                .toList();
    }

    // A busca cobre só o nome, e não o tipo: o membro de elenco não tem descrição, e o tipo é valor
    // fechado — filtrar por ele é argumento próprio, não texto livre.
    @Override
    public Pagination<CastMember> findAll(final SearchQuery query) {
        final var page = PageRequest.of(query.page(), query.perPage(), sortOf(query));
        final var result = this.searchOperations.search(queryOf(query, page), CastMemberDocument.class);
        final var items = result.stream()
                .map(SearchHit::getContent)
                .map(CastMemberDocument::toCastMember)
                .toList();
        return new Pagination<>(query.page(), query.perPage(), result.getTotalHits(), items);
    }

    // Só o que está ativo, como na categoria: desativar no admin-codeflix tira do catálogo sem
    // apagar o registro.
    private static Query queryOf(final SearchQuery query, final PageRequest page) {
        final var active = new Criteria(ACTIVE).is(true);
        final var terms = query.terms();
        if (terms == null || terms.isBlank()) {
            return new CriteriaQuery(active, page);
        }
        return new CriteriaQuery(active.subCriteria(SearchTerms.across(terms, NAME)), page);
    }

    private static Sort sortOf(final SearchQuery query) {
        final var field = NAME.equals(query.sort()) ? NAME + KEYWORD_SUFFIX : query.sort();
        return Sort.by(Sort.Direction.fromString(query.direction()), field);
    }
}
