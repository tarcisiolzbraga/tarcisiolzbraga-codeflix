package com.tarcisiolzbraga.codeflix.admin.application.video.create;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

import com.tarcisiolzbraga.codeflix.admin.application.video.VideoFields;
import com.tarcisiolzbraga.codeflix.admin.application.video.VideoReferenceIds;
import com.tarcisiolzbraga.codeflix.admin.domain.castmember.CastMember;
import com.tarcisiolzbraga.codeflix.admin.domain.castmember.CastMemberGateway;
import com.tarcisiolzbraga.codeflix.admin.domain.castmember.CastMemberType;
import com.tarcisiolzbraga.codeflix.admin.domain.category.Category;
import com.tarcisiolzbraga.codeflix.admin.domain.category.CategoryGateway;
import com.tarcisiolzbraga.codeflix.admin.domain.category.CategoryID;
import com.tarcisiolzbraga.codeflix.admin.domain.genre.Genre;
import com.tarcisiolzbraga.codeflix.admin.domain.genre.GenreGateway;
import com.tarcisiolzbraga.codeflix.admin.domain.validation.ValidationError;
import com.tarcisiolzbraga.codeflix.admin.domain.video.Rating;
import com.tarcisiolzbraga.codeflix.admin.domain.video.Video;
import com.tarcisiolzbraga.codeflix.admin.domain.video.VideoGateway;
import com.tarcisiolzbraga.codeflix.admin.domain.video.VideoID;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.IntegrationTest;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.video.persistence.VideoRepository;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

@IntegrationTest
class CreateVideoUseCaseIT {

    private static final String EXPECTED_TITLE = "Duna";
    private static final String EXPECTED_DESCRIPTION = "Paul Atreides em Arrakis";

    @Autowired
    private CreateVideoUseCase useCase;

    @Autowired
    private CategoryGateway categoryGateway;

    @Autowired
    private GenreGateway genreGateway;

    @Autowired
    private CastMemberGateway castMemberGateway;

    @Autowired
    private VideoRepository videoRepository;

    @MockitoSpyBean
    private VideoGateway videoGateway;

    @Test
    void givenValidCommandWithTheThreeReferences_whenCallExecute_thenPersistTheVideoWithThem() {
        final var category = this.categoryGateway.create(Category.newCategory("Filmes", null, true)).getId();
        final var genre = this.genreGateway.create(Genre.newGenre("Ficção", true)).getId();
        final var member = this.castMemberGateway
                .create(CastMember.newCastMember("Timothée Chalamet", CastMemberType.ACTOR, true))
                .getId();
        final var command = CreateVideoCommand.with(
                fields("12"),
                new VideoReferenceIds(
                        Set.of(category.getValue()), Set.of(genre.getValue()), Set.of(member.getValue())));

        final var actualResult = this.useCase.execute(command);

        assertTrue(actualResult.isRight());
        final var persisted = reload(actualResult.get().id());
        assertEquals(EXPECTED_TITLE, persisted.getTitle());
        assertEquals(Rating.AGE_12, persisted.getRating());
        assertEquals(Set.of(category), persisted.getCategories());
        assertEquals(Set.of(genre), persisted.getGenres());
        assertEquals(Set.of(member), persisted.getCastMembers());
        assertTrue(persisted.isActive());
    }

    @Test
    void givenCommandWithoutReferences_whenCallExecute_thenPersistItEmpty() {
        final var command = CreateVideoCommand.with(fields("L"), VideoReferenceIds.none());

        final var actualResult = this.useCase.execute(command);

        final var persisted = reload(actualResult.get().id());
        assertEquals(Rating.L, persisted.getRating());
        assertTrue(persisted.getCategories().isEmpty());
        assertTrue(persisted.getGenres().isEmpty());
        assertTrue(persisted.getCastMembers().isEmpty());
    }

    @Test
    void givenUnknownReferenceAndInvalidRating_whenCallExecute_thenReturnBothErrorsAndPersistNothing() {
        final var unknown = CategoryID.unique().getValue();
        final var command = CreateVideoCommand.with(
                fields("99"), new VideoReferenceIds(Set.of(unknown), Set.of(), Set.of()));

        final var actualResult = this.useCase.execute(command);

        assertEquals(
                List.of("Some categories could not be found: " + unknown, "'rating' should not be null"),
                actualResult.getLeft().getErrors().stream()
                        .map(ValidationError::message)
                        .toList());
        assertEquals(0, this.videoRepository.count());
    }

    @Test
    void givenFailingGateway_whenCallExecute_thenPropagateTheExceptionInsteadOfValidationError() {
        final var command = CreateVideoCommand.with(fields("12"), VideoReferenceIds.none());
        final var expectedException = new IllegalStateException("banco indisponível");
        doThrow(expectedException).when(this.videoGateway).create(any());

        final var actualException = assertThrows(IllegalStateException.class, () -> this.useCase.execute(command));

        assertSame(expectedException, actualException);
        assertEquals(0, this.videoRepository.count());
    }

    private VideoFields fields(final String rating) {
        return new VideoFields(EXPECTED_TITLE, EXPECTED_DESCRIPTION, 2021, 155.0, rating);
    }

    private Video reload(final String id) {
        return this.videoGateway.findById(VideoID.from(id)).orElseThrow();
    }
}
