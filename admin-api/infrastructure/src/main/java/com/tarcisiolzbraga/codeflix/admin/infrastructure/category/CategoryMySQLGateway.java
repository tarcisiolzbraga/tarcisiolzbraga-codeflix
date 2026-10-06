package com.tarcisiolzbraga.codeflix.admin.infrastructure.category;

import com.tarcisiolzbraga.codeflix.admin.domain.category.Category;
import com.tarcisiolzbraga.codeflix.admin.domain.category.CategoryGateway;
import com.tarcisiolzbraga.codeflix.admin.domain.category.CategoryID;
import com.tarcisiolzbraga.codeflix.admin.domain.pagination.Pagination;
import com.tarcisiolzbraga.codeflix.admin.domain.pagination.SearchQuery;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.category.persistence.CategoryJpaEntity;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.category.persistence.CategoryRepository;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.util.SpecificationUtils;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

@Component
public class CategoryMySQLGateway implements CategoryGateway {

    private final CategoryRepository categoryRepository;

    public CategoryMySQLGateway(final CategoryRepository categoryRepository) {
        this.categoryRepository = Objects.requireNonNull(categoryRepository, "'categoryRepository' should not be null");
    }

    @Override
    public Category create(final Category category) {
        return save(category);
    }

    @Override
    public Category update(final Category category) {
        return save(category);
    }

    @Override
    public void deleteById(final CategoryID id) {
        final var value = id.getValue();
        if (this.categoryRepository.existsById(value)) {
            this.categoryRepository.deleteById(value);
        }
    }

    @Override
    public Optional<Category> findById(final CategoryID id) {
        return this.categoryRepository.findById(id.getValue()).map(CategoryJpaEntity::toAggregate);
    }

    @Override
    public Pagination<Category> findAll(final SearchQuery query) {
        final var page = PageRequest.of(query.page(), query.perPage(), sortOf(query));
        final var result = this.categoryRepository.findAll(specificationOf(query), page);
        return new Pagination<>(
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.map(CategoryJpaEntity::toAggregate).toList());
    }

    @Override
    public Set<CategoryID> findExistingIds(final Set<CategoryID> ids) {
        if (ids.isEmpty()) {
            return Set.of();
        }
        final var values = ids.stream().map(CategoryID::getValue).collect(Collectors.toSet());
        return this.categoryRepository.findExistingIds(values).stream()
                .map(CategoryID::from)
                .collect(Collectors.toUnmodifiableSet());
    }

    private Category save(final Category category) {
        return this.categoryRepository.save(CategoryJpaEntity.from(category)).toAggregate();
    }

    private Sort sortOf(final SearchQuery query) {
        return Sort.by(Sort.Direction.fromString(query.direction()), query.sort());
    }

    private Specification<CategoryJpaEntity> specificationOf(final SearchQuery query) {
        return Optional.ofNullable(query.terms())
                .filter(terms -> !terms.isBlank())
                .<Specification<CategoryJpaEntity>>map(terms -> SpecificationUtils
                        .<CategoryJpaEntity>like("name", terms)
                        .or(SpecificationUtils.like("description", terms)))
                .orElse(null);
    }
}
