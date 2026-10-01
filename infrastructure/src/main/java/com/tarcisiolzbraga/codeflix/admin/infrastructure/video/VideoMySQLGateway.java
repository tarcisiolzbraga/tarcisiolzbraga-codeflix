package com.tarcisiolzbraga.codeflix.admin.infrastructure.video;

import com.tarcisiolzbraga.codeflix.admin.domain.Identifier;
import com.tarcisiolzbraga.codeflix.admin.domain.events.DomainEventPublisher;
import com.tarcisiolzbraga.codeflix.admin.domain.castmember.CastMemberID;
import com.tarcisiolzbraga.codeflix.admin.domain.category.CategoryID;
import com.tarcisiolzbraga.codeflix.admin.domain.genre.GenreID;
import com.tarcisiolzbraga.codeflix.admin.domain.pagination.Pagination;
import com.tarcisiolzbraga.codeflix.admin.domain.pagination.SearchQuery;
import com.tarcisiolzbraga.codeflix.admin.domain.video.Video;
import com.tarcisiolzbraga.codeflix.admin.domain.video.VideoGateway;
import com.tarcisiolzbraga.codeflix.admin.domain.video.VideoID;
import com.tarcisiolzbraga.codeflix.admin.domain.video.VideoPreview;
import com.tarcisiolzbraga.codeflix.admin.domain.video.VideoSearchQuery;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.video.persistence.VideoJpaEntity;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.video.persistence.VideoRepository;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class VideoMySQLGateway implements VideoGateway {

    private static final String LIKE_WILDCARD = "%";

    private final VideoRepository videoRepository;
    private final DomainEventPublisher eventPublisher;

    public VideoMySQLGateway(
            final VideoRepository videoRepository, final DomainEventPublisher eventPublisher) {
        this.videoRepository = Objects.requireNonNull(videoRepository, "'videoRepository' should not be null");
        this.eventPublisher = Objects.requireNonNull(eventPublisher, "'eventPublisher' should not be null");
    }

    // Transacional porque salvar o vídeo e gravar o aviso na tabela de saída precisam acontecer
    // juntos: é isso que impede a linha existir sem o aviso, ou o contrário.
    @Override
    @Transactional
    public Video create(final Video video) {
        return save(video);
    }

    @Override
    @Transactional
    public Video update(final Video video) {
        return save(video);
    }

    @Override
    public void deleteById(final VideoID id) {
        final var value = id.getValue();
        if (this.videoRepository.existsById(value)) {
            this.videoRepository.deleteById(value);
        }
    }

    @Override
    public Optional<Video> findById(final VideoID id) {
        return this.videoRepository.findById(id.getValue()).map(VideoJpaEntity::toAggregate);
    }

    @Override
    public Pagination<VideoPreview> findAll(final VideoSearchQuery query) {
        final var page = query.page();
        final var result = this.videoRepository.findAll(
                termsOf(page),
                nullIfEmpty(valuesOf(query.categories())),
                nullIfEmpty(valuesOf(query.genres())),
                nullIfEmpty(valuesOf(query.castMembers())),
                PageRequest.of(page.page(), page.perPage(), sortOf(page)));
        return new Pagination<>(
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.toList());
    }

    @Override
    public boolean existsByCategory(final CategoryID categoryId) {
        return this.videoRepository.existsByCategoryId(categoryId.getValue());
    }

    @Override
    public boolean existsByGenre(final GenreID genreId) {
        return this.videoRepository.existsByGenreId(genreId.getValue());
    }

    @Override
    public boolean existsByCastMember(final CastMemberID castMemberId) {
        return this.videoRepository.existsByCastMemberId(castMemberId.getValue());
    }

    // O aviso é gravado junto, na mesma transação, e sai uma vez: quem publica esvazia a lista do
    // agregado, então salvar de novo não repete o aviso.
    private Video save(final Video video) {
        final var saved = this.videoRepository.save(VideoJpaEntity.from(video)).toAggregate();
        video.publishDomainEvents(this.eventPublisher);
        return saved;
    }

    private Sort sortOf(final SearchQuery page) {
        return Sort.by(Sort.Direction.fromString(page.direction()), page.sort());
    }

    // A consulta compara com o título em maiúsculas, então o termo vai no mesmo formato.
    private String termsOf(final SearchQuery page) {
        return Optional.ofNullable(page.terms())
                .filter(terms -> !terms.isBlank())
                .map(terms -> LIKE_WILDCARD + terms.toUpperCase() + LIKE_WILDCARD)
                .orElse(null);
    }

    private Set<String> valuesOf(final Set<? extends Identifier> ids) {
        return ids.stream().map(Identifier::getValue).collect(Collectors.toUnmodifiableSet());
    }

    // Conjunto vazio significaria "nenhum resultado"; nulo é o que a consulta entende como sem filtro.
    private Set<String> nullIfEmpty(final Set<String> values) {
        return values.isEmpty() ? null : values;
    }
}
