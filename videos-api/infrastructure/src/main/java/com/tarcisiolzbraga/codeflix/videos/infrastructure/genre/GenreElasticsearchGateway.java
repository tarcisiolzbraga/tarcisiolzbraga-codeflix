package com.tarcisiolzbraga.codeflix.videos.infrastructure.genre;

import com.tarcisiolzbraga.codeflix.videos.domain.category.CategoryID;
import com.tarcisiolzbraga.codeflix.videos.domain.genre.Genre;
import com.tarcisiolzbraga.codeflix.videos.domain.genre.GenreGateway;
import com.tarcisiolzbraga.codeflix.videos.domain.genre.GenreID;
import com.tarcisiolzbraga.codeflix.videos.domain.genre.GenreSearchQuery;
import com.tarcisiolzbraga.codeflix.videos.domain.pagination.Pagination;
import com.tarcisiolzbraga.codeflix.videos.infrastructure.elasticsearch.SearchTerms;
import com.tarcisiolzbraga.codeflix.videos.infrastructure.genre.persistence.GenreDocument;
import com.tarcisiolzbraga.codeflix.videos.infrastructure.genre.persistence.GenreRepository;
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
public class GenreElasticsearchGateway implements GenreGateway {

    private static final String NAME = "name";
    private static final String ACTIVE = "active";
    private static final String CATEGORIES = "categories";
    private static final String KEYWORD_SUFFIX = ".keyword";

    private final GenreRepository genreRepository;
    private final SearchOperations searchOperations;

    public GenreElasticsearchGateway(
            final GenreRepository genreRepository, final SearchOperations searchOperations) {
        this.genreRepository = Objects.requireNonNull(genreRepository, "'genreRepository' should not be null");
        this.searchOperations = Objects.requireNonNull(searchOperations, "'searchOperations' should not be null");
    }

    @Override
    public Genre save(final Genre genre) {
        this.genreRepository.save(GenreDocument.from(genre));
        return genre;
    }

    @Override
    public void deleteById(final GenreID id) {
        this.genreRepository.deleteById(id.value());
    }

    // Vazio para o inativo: nada inativo é devolvido em leitura alguma.
    @Override
    public Optional<Genre> findById(final GenreID id) {
        return this.genreRepository
                .findById(id.value())
                .filter(GenreDocument::isActive)
                .map(GenreDocument::toGenre);
    }

    @Override
    public List<Genre> findAllById(final Set<GenreID> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        final var values = ids.stream().map(GenreID::value).toList();
        // Resolução de relação: um vídeo ativo com gêneros inativos vem sem eles.
        return StreamSupport.stream(this.genreRepository.findAllById(values).spliterator(), false)
                .filter(GenreDocument::isActive)
                .map(GenreDocument::toGenre)
                .toList();
    }

    @Override
    public Pagination<Genre> findAll(final GenreSearchQuery query) {
        final var page = PageRequest.of(query.page(), query.perPage(), sortOf(query));
        final var result = this.searchOperations.search(queryOf(query, page), GenreDocument.class);
        final var items = result.stream()
                .map(SearchHit::getContent)
                .map(GenreDocument::toGenre)
                .toList();
        return new Pagination<>(query.page(), query.perPage(), result.getTotalHits(), items);
    }

    // Os filtros entram como subcriteria, um por vez, para cada um ficar agrupado: encadeá-los no
    // mesmo nível deixaria o resultado à mercê da precedência entre and e or.
    private static Query queryOf(final GenreSearchQuery query, final PageRequest page) {
        var criteria = new Criteria(ACTIVE).is(true);
        final var terms = query.terms();
        if (terms != null && !terms.isBlank()) {
            criteria = criteria.subCriteria(SearchTerms.across(terms, NAME));
        }
        if (!query.categories().isEmpty()) {
            final var values = query.categories().stream().map(CategoryID::value).toList();
            criteria = criteria.subCriteria(new Criteria(CATEGORIES).in(values));
        }
        return new CriteriaQuery(criteria, page);
    }

    private static Sort sortOf(final GenreSearchQuery query) {
        final var field = NAME.equals(query.sort()) ? NAME + KEYWORD_SUFFIX : query.sort();
        return Sort.by(Sort.Direction.fromString(query.direction()), field);
    }
}
