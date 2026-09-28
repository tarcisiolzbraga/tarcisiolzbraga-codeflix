package com.tarcisiolzbraga.codeflix.admin.application.video.get;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tarcisiolzbraga.codeflix.admin.domain.category.Category;
import com.tarcisiolzbraga.codeflix.admin.domain.category.CategoryGateway;
import com.tarcisiolzbraga.codeflix.admin.domain.exceptions.NotFoundException;
import com.tarcisiolzbraga.codeflix.admin.domain.video.Rating;
import com.tarcisiolzbraga.codeflix.admin.domain.video.Video;
import com.tarcisiolzbraga.codeflix.admin.domain.video.VideoDetails;
import com.tarcisiolzbraga.codeflix.admin.domain.video.VideoGateway;
import com.tarcisiolzbraga.codeflix.admin.domain.video.VideoID;
import com.tarcisiolzbraga.codeflix.admin.domain.video.VideoReferences;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.IntegrationTest;
import java.time.Year;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@IntegrationTest
class GetVideoByIdUseCaseIT {

    private static final String EXPECTED_TITLE = "Duna";

    @Autowired
    private GetVideoByIdUseCase useCase;

    @Autowired
    private CategoryGateway categoryGateway;

    @Autowired
    private VideoGateway videoGateway;

    @Test
    void givenPersistedVideo_whenCallExecute_thenReturnItWithReferencesAndTimestamps() {
        final var category = this.categoryGateway.create(Category.newCategory("Filmes", null, true)).getId();
        final var video = this.videoGateway.create(Video.newVideo(
                VideoDetails.with(EXPECTED_TITLE, "Arrakis", Year.of(2021), 155.0, Rating.AGE_12),
                VideoReferences.with(Set.of(category), Set.of(), Set.of())));

        final var actualOutput = this.useCase.execute(video.getId().getValue());

        assertEquals(video.getId().getValue(), actualOutput.id());
        assertEquals(EXPECTED_TITLE, actualOutput.fields().title());
        assertEquals(2021, actualOutput.fields().launchedAt());
        assertEquals("12", actualOutput.fields().rating());
        assertEquals(Set.of(category.getValue()), actualOutput.references().categories());
        assertEquals(video.getCreatedAt(), actualOutput.createdAt());
        assertTrue(actualOutput.active());
        assertFalse(actualOutput.published());
    }

    @Test
    void givenUnknownId_whenCallExecute_thenThrowNotFound() {
        final var expectedId = VideoID.unique();

        final var actualException =
                assertThrows(NotFoundException.class, () -> this.useCase.execute(expectedId.getValue()));

        assertEquals("Video with ID %s was not found".formatted(expectedId.getValue()), actualException.getMessage());
    }
}
