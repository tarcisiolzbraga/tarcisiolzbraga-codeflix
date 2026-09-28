package com.tarcisiolzbraga.codeflix.admin.application.video.close;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.tarcisiolzbraga.codeflix.admin.domain.exceptions.NotFoundException;
import com.tarcisiolzbraga.codeflix.admin.domain.video.Rating;
import com.tarcisiolzbraga.codeflix.admin.domain.video.Video;
import com.tarcisiolzbraga.codeflix.admin.domain.video.VideoDetails;
import com.tarcisiolzbraga.codeflix.admin.domain.video.VideoGateway;
import com.tarcisiolzbraga.codeflix.admin.domain.video.VideoID;
import com.tarcisiolzbraga.codeflix.admin.domain.video.VideoReferences;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.IntegrationTest;
import java.time.Year;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@IntegrationTest
class CloseVideoUseCaseIT {

    @Autowired
    private CloseVideoUseCase useCase;

    @Autowired
    private VideoGateway videoGateway;

    @Test
    void givenVideoInTheOtherState_whenCallExecute_thenPersistTheChange() {
        final var video = givenPersistedVideo(true);

        this.useCase.execute(video.getId().getValue());

        assertFalse(reload(video).isOpened());
    }

    @Test
    void givenVideoAlreadyInTheState_whenCallExecute_thenKeepIt() {
        final var video = givenPersistedVideo(false);

        this.useCase.execute(video.getId().getValue());

        assertFalse(reload(video).isOpened());
    }

    @Test
    void givenUnknownId_whenCallExecute_thenThrowNotFound() {
        final var expectedId = VideoID.unique();

        final var actualException =
                assertThrows(NotFoundException.class, () -> this.useCase.execute(expectedId.getValue()));

        assertEquals("Video with ID %s was not found".formatted(expectedId.getValue()), actualException.getMessage());
    }

    // O vídeo nasce ativo, fechado e não publicado; o parâmetro põe ele no estado que o teste precisa.
    private Video givenPersistedVideo(final boolean opened) {
        final var video = Video.newVideo(
                VideoDetails.with("Duna", "Arrakis", Year.of(2021), 155.0, Rating.AGE_12), VideoReferences.none());
        if (opened) {
            video.open();
        }
        return this.videoGateway.create(video);
    }

    private Video reload(final Video video) {
        return this.videoGateway.findById(video.getId()).orElseThrow();
    }
}
