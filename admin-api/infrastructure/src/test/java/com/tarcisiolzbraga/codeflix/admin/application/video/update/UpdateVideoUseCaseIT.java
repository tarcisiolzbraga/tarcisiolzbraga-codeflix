package com.tarcisiolzbraga.codeflix.admin.application.video.update;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tarcisiolzbraga.codeflix.admin.application.video.VideoFields;
import com.tarcisiolzbraga.codeflix.admin.application.video.VideoReferenceIds;
import com.tarcisiolzbraga.codeflix.admin.domain.category.Category;
import com.tarcisiolzbraga.codeflix.admin.domain.category.CategoryGateway;
import com.tarcisiolzbraga.codeflix.admin.domain.category.CategoryID;
import com.tarcisiolzbraga.codeflix.admin.domain.exceptions.NotFoundException;
import com.tarcisiolzbraga.codeflix.admin.domain.validation.ValidationError;
import com.tarcisiolzbraga.codeflix.admin.domain.video.Rating;
import com.tarcisiolzbraga.codeflix.admin.domain.video.Video;
import com.tarcisiolzbraga.codeflix.admin.domain.video.VideoFixture;
import com.tarcisiolzbraga.codeflix.admin.domain.video.VideoGateway;
import com.tarcisiolzbraga.codeflix.admin.domain.video.VideoID;
import com.tarcisiolzbraga.codeflix.admin.domain.video.VideoReferences;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.IntegrationTest;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@IntegrationTest
class UpdateVideoUseCaseIT {

    private static final String OLD_TITLE = "Duna";
    private static final String EXPECTED_TITLE = "Duna: Parte 2";

    @Autowired
    private UpdateVideoUseCase useCase;

    @Autowired
    private CategoryGateway categoryGateway;

    @Autowired
    private VideoGateway videoGateway;

    @Test
    void givenPersistedVideo_whenCallExecute_thenReplaceFieldsAndReferences() {
        final var video = givenPersistedVideo();
        final var category = this.categoryGateway.create(Category.newCategory("Filmes", null, true)).getId();
        final var command = UpdateVideoCommand.with(
                video.getId().getValue(),
                new VideoFields(EXPECTED_TITLE, "Arrakis", 2024, 166.0, "14"),
                new VideoReferenceIds(Set.of(category.getValue()), Set.of(), Set.of()));

        final var actualResult = this.useCase.execute(command);

        assertTrue(actualResult.isRight());
        final var persisted = reload(video);
        assertEquals(EXPECTED_TITLE, persisted.getTitle());
        assertEquals(Rating.AGE_14, persisted.getRating());
        assertEquals(Set.of(category), persisted.getCategories());
    }

    @Test
    void givenPersistedVideo_whenCallExecute_thenKeepCreatedAtAndAdvanceUpdatedAt() {
        final var video = givenPersistedVideo();
        final var command = UpdateVideoCommand.with(
                video.getId().getValue(),
                new VideoFields(EXPECTED_TITLE, "Arrakis", 2021, 155.0, "12"),
                VideoReferenceIds.none());

        this.useCase.execute(command);

        final var persisted = reload(video);
        assertEquals(video.getCreatedAt(), persisted.getCreatedAt());
        assertTrue(video.getUpdatedAt().isBefore(persisted.getUpdatedAt()));
    }

    @Test
    void givenUnknownCategory_whenCallExecute_thenReturnLeftAndKeepTheStoredValues() {
        final var video = givenPersistedVideo();
        final var unknown = CategoryID.unique().getValue();
        final var command = UpdateVideoCommand.with(
                video.getId().getValue(),
                new VideoFields(EXPECTED_TITLE, "Arrakis", 2024, 166.0, "14"),
                new VideoReferenceIds(Set.of(unknown), Set.of(), Set.of()));

        final var actualResult = this.useCase.execute(command);

        assertEquals(
                List.of("Some categories could not be found: " + unknown),
                actualResult.getLeft().getErrors().stream()
                        .map(ValidationError::message)
                        .toList());
        assertEquals(OLD_TITLE, reload(video).getTitle());
    }

    @Test
    void givenUnknownId_whenCallExecute_thenThrowNotFound() {
        final var expectedId = VideoID.unique();
        final var command = UpdateVideoCommand.with(
                expectedId.getValue(),
                new VideoFields(EXPECTED_TITLE, "Arrakis", 2024, 166.0, "14"),
                VideoReferenceIds.none());

        final var actualException = assertThrows(NotFoundException.class, () -> this.useCase.execute(command));

        assertEquals("Video with ID %s was not found".formatted(expectedId.getValue()), actualException.getMessage());
    }

    private Video givenPersistedVideo() {
        return this.videoGateway.create(Video.newVideo(
                VideoFixture.details(OLD_TITLE),
                VideoReferences.none()));
    }

    private Video reload(final Video video) {
        return this.videoGateway.findById(video.getId()).orElseThrow();
    }
}
