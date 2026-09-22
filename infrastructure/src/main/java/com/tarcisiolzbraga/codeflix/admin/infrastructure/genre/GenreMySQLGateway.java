package com.tarcisiolzbraga.codeflix.admin.infrastructure.genre;

import com.tarcisiolzbraga.codeflix.admin.domain.genre.Genre;
import com.tarcisiolzbraga.codeflix.admin.domain.genre.GenreGateway;
import com.tarcisiolzbraga.codeflix.admin.domain.genre.GenreID;
import com.tarcisiolzbraga.codeflix.admin.domain.pagination.Pagination;
import com.tarcisiolzbraga.codeflix.admin.domain.pagination.SearchQuery;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.genre.persistence.GenreJpaEntity;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.genre.persistence.GenreRepository;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.util.SpecificationUtils;
import java.util.Objects;
import java.util.Optional;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

@Component
public class GenreMySQLGateway implements GenreGateway {

    private final GenreRepository genreRepository;

    public GenreMySQLGateway(final GenreRepository genreRepository) {
        this.genreRepository = Objects.requireNonNull(genreRepository, "'genreRepository' should not be null");
    }

    @Override
    public Genre create(final Genre genre) {
        return save(genre);
    }

    @Override
    public Genre update(final Genre genre) {
        return save(genre);
    }

    @Override
    public void deleteById(final GenreID id) {
        final var value = id.getValue();
        if (this.genreRepository.existsById(value)) {
            this.genreRepository.deleteById(value);
        }
    }

    @Override
    public Optional<Genre> findById(final GenreID id) {
        return this.genreRepository.findById(id.getValue()).map(GenreJpaEntity::toAggregate);
    }

    @Override
    public Pagination<Genre> findAll(final SearchQuery query) {
        final var page = PageRequest.of(query.page(), query.perPage(), sortOf(query));
        final var result = this.genreRepository.findAll(specificationOf(query), page);
        return new Pagination<>(
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.map(GenreJpaEntity::toAggregate).toList());
    }

    private Genre save(final Genre genre) {
        return this.genreRepository.save(GenreJpaEntity.from(genre)).toAggregate();
    }

    private Sort sortOf(final SearchQuery query) {
        return Sort.by(Sort.Direction.fromString(query.direction()), query.sort());
    }

    private Specification<GenreJpaEntity> specificationOf(final SearchQuery query) {
        return Optional.ofNullable(query.terms())
                .filter(terms -> !terms.isBlank())
                .map(terms -> SpecificationUtils.<GenreJpaEntity>like("name", terms))
                .orElse(null);
    }
}
