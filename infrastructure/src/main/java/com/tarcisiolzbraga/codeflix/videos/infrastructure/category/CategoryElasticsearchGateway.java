package com.tarcisiolzbraga.codeflix.videos.infrastructure.category;

import com.tarcisiolzbraga.codeflix.videos.infrastructure.elasticsearch.SearchTerms;
import static org.springframework.data.elasticsearch.core.query.Criteria.where;

import com.tarcisiolzbraga.codeflix.videos.domain.category.Category;
import com.tarcisiolzbraga.codeflix.videos.domain.category.CategoryGateway;
import com.tarcisiolzbraga.codeflix.videos.domain.category.CategoryID;
import com.tarcisiolzbraga.codeflix.videos.domain.pagination.Pagination;
import com.tarcisiolzbraga.codeflix.videos.domain.pagination.SearchQuery;
import com.tarcisiolzbraga.codeflix.videos.infrastructure.category.persistence.CategoryDocument;
import com.tarcisiolzbraga.codeflix.videos.infrastructure.category.persistence.CategoryRepository;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.StreamSupport;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchOperations;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.stereotype.Component;

@Component
public class CategoryElasticsearchGateway implements CategoryGateway {

    private static final String NAME = "name";
    private static final String DESCRIPTION = "description";
    private static final String KEYWORD_SUFFIX = ".keyword";

    private final CategoryRepository categoryRepository;
    private final SearchOperations searchOperations;

    public CategoryElasticsearchGateway(
            final CategoryRepository categoryRepository, final SearchOperations searchOperations) {
        this.categoryRepository = Objects.requireNonNull(categoryRepository, "'categoryRepository' should not be null");
        this.searchOperations = Objects.requireNonNull(searchOperations, "'searchOperations' should not be null");
    }

    @Override
    public Category save(final Category category) {
        this.categoryRepository.save(CategoryDocument.from(category));
        return category;
    }

    @Override
    public void deleteById(final CategoryID id) {
        this.categoryRepository.deleteById(id.getValue());
    }

    @Override
    public Optional<Category> findById(final CategoryID id) {
        return this.categoryRepository.findById(id.getValue()).map(CategoryDocument::toCategory);
    }

    @Override
    public List<Category> findAllById(final Set<CategoryID> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        final var values = ids.stream().map(CategoryID::getValue).toList();
        return StreamSupport.stream(this.categoryRepository.findAllById(values).spliterator(), false)
                .map(CategoryDocument::toCategory)
                .toList();
    }

    @Override
    public Pagination<Category> findAll(final SearchQuery query) {
        final var page = PageRequest.of(query.page(), query.perPage(), sortOf(query));
        final var result = this.searchOperations.search(queryOf(query, page), CategoryDocument.class);
        final var items = result.stream()
                .map(SearchHit::getContent)
                .map(CategoryDocument::toCategory)
                .toList();
        return new Pagination<>(query.page(), query.perPage(), result.getTotalHits(), items);
    }

    private static Query queryOf(final SearchQuery query, final PageRequest page) {
        final var terms = query.terms();
        if (terms == null || terms.isBlank()) {
            return Query.findAll().setPageable(page);
        }
        return new CriteriaQuery(SearchTerms.across(terms, NAME, DESCRIPTION), page);
    }

    // Ordenar por um campo analisado não funciona: o Elasticsearch ordenaria pelos termos quebrados.
    // Para o nome, a ordenação vai no subcampo keyword, que guarda o valor inteiro.
    private static Sort sortOf(final SearchQuery query) {
        final var field = NAME.equals(query.sort()) ? NAME + KEYWORD_SUFFIX : query.sort();
        return Sort.by(Sort.Direction.fromString(query.direction()), field);
    }
}
