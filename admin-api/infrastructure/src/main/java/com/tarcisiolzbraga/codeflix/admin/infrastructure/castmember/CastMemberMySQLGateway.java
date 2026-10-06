package com.tarcisiolzbraga.codeflix.admin.infrastructure.castmember;

import com.tarcisiolzbraga.codeflix.admin.domain.castmember.CastMember;
import com.tarcisiolzbraga.codeflix.admin.domain.castmember.CastMemberGateway;
import com.tarcisiolzbraga.codeflix.admin.domain.castmember.CastMemberID;
import com.tarcisiolzbraga.codeflix.admin.domain.pagination.Pagination;
import com.tarcisiolzbraga.codeflix.admin.domain.pagination.SearchQuery;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.castmember.persistence.CastMemberJpaEntity;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.castmember.persistence.CastMemberRepository;
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
public class CastMemberMySQLGateway implements CastMemberGateway {

    private final CastMemberRepository castMemberRepository;

    public CastMemberMySQLGateway(final CastMemberRepository castMemberRepository) {
        this.castMemberRepository =
                Objects.requireNonNull(castMemberRepository, "'castMemberRepository' should not be null");
    }

    @Override
    public CastMember create(final CastMember castMember) {
        return save(castMember);
    }

    @Override
    public CastMember update(final CastMember castMember) {
        return save(castMember);
    }

    @Override
    public void deleteById(final CastMemberID id) {
        final var value = id.getValue();
        if (this.castMemberRepository.existsById(value)) {
            this.castMemberRepository.deleteById(value);
        }
    }

    @Override
    public Optional<CastMember> findById(final CastMemberID id) {
        return this.castMemberRepository.findById(id.getValue()).map(CastMemberJpaEntity::toAggregate);
    }

    @Override
    public Pagination<CastMember> findAll(final SearchQuery query) {
        final var page = PageRequest.of(query.page(), query.perPage(), sortOf(query));
        final var result = this.castMemberRepository.findAll(specificationOf(query), page);
        return new Pagination<>(
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.map(CastMemberJpaEntity::toAggregate).toList());
    }

    @Override
    public Set<CastMemberID> findExistingIds(final Set<CastMemberID> ids) {
        if (ids.isEmpty()) {
            return Set.of();
        }
        final var values = ids.stream().map(CastMemberID::getValue).collect(Collectors.toSet());
        return this.castMemberRepository.findExistingIds(values).stream()
                .map(CastMemberID::from)
                .collect(Collectors.toUnmodifiableSet());
    }

    private CastMember save(final CastMember castMember) {
        return this.castMemberRepository
                .save(CastMemberJpaEntity.from(castMember))
                .toAggregate();
    }

    private Sort sortOf(final SearchQuery query) {
        return Sort.by(Sort.Direction.fromString(query.direction()), query.sort());
    }

    private Specification<CastMemberJpaEntity> specificationOf(final SearchQuery query) {
        return Optional.ofNullable(query.terms())
                .filter(terms -> !terms.isBlank())
                .map(terms -> SpecificationUtils.<CastMemberJpaEntity>like("name", terms))
                .orElse(null);
    }
}
