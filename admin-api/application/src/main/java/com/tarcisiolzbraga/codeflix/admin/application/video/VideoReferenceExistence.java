package com.tarcisiolzbraga.codeflix.admin.application.video;

import com.tarcisiolzbraga.codeflix.admin.application.ReferenceExistenceValidator;
import com.tarcisiolzbraga.codeflix.admin.domain.castmember.CastMemberGateway;
import com.tarcisiolzbraga.codeflix.admin.domain.castmember.CastMemberID;
import com.tarcisiolzbraga.codeflix.admin.domain.category.CategoryGateway;
import com.tarcisiolzbraga.codeflix.admin.domain.category.CategoryID;
import com.tarcisiolzbraga.codeflix.admin.domain.genre.GenreGateway;
import com.tarcisiolzbraga.codeflix.admin.domain.genre.GenreID;
import com.tarcisiolzbraga.codeflix.admin.domain.validation.ValidationHandler;
import com.tarcisiolzbraga.codeflix.admin.domain.video.VideoReferences;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

// Os três agregados que o vídeo referencia são conferidos de uma vez, cada um pelo gateway dele, e
// os que faltam vão para a mesma Notification da validação do vídeo.
public class VideoReferenceExistence {

    private final ReferenceExistenceValidator<CategoryID> categories;
    private final ReferenceExistenceValidator<GenreID> genres;
    private final ReferenceExistenceValidator<CastMemberID> castMembers;

    public VideoReferenceExistence(
            final CategoryGateway categoryGateway,
            final GenreGateway genreGateway,
            final CastMemberGateway castMemberGateway) {
        this.categories = new ReferenceExistenceValidator<>("categories", categoryGateway::findExistingIds);
        this.genres = new ReferenceExistenceValidator<>("genres", genreGateway::findExistingIds);
        this.castMembers = new ReferenceExistenceValidator<>("cast members", castMemberGateway::findExistingIds);
    }

    public VideoReferences validate(final VideoReferenceIds ids, final ValidationHandler handler) {
        final var categoryIds = toIds(ids.categories(), CategoryID::from);
        final var genreIds = toIds(ids.genres(), GenreID::from);
        final var memberIds = toIds(ids.castMembers(), CastMemberID::from);
        this.categories.validate(categoryIds, handler);
        this.genres.validate(genreIds, handler);
        this.castMembers.validate(memberIds, handler);
        return VideoReferences.with(categoryIds, genreIds, memberIds);
    }

    private <ID> Set<ID> toIds(final Set<String> values, final Function<String, ID> factory) {
        return values.stream().map(factory).collect(Collectors.toUnmodifiableSet());
    }
}
