package com.tarcisiolzbraga.codeflix.videos.domain.video;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tarcisiolzbraga.codeflix.videos.domain.category.CategoryID;
import com.tarcisiolzbraga.codeflix.videos.domain.exceptions.DomainException;
import com.tarcisiolzbraga.codeflix.videos.domain.validation.handler.Notification;
import com.tarcisiolzbraga.codeflix.videos.domain.validation.handler.ThrowsValidationHandler;
import java.time.Instant;
import java.time.Year;
import java.util.Set;
import org.junit.jupiter.api.Test;

class VideoTest {

    private static final VideoID EXPECTED_ID = VideoID.from("9e2c1b4a-7d3f-4e80-8a51-c2b3d4e5f608");
    private static final String EXPECTED_TITLE = "Duna";
    private static final String EXPECTED_DESCRIPTION = "Paul Atreides em Arrakis";
    private static final Instant CREATED_AT = Instant.parse("2026-09-30T12:00:00Z");
    private static final Instant UPDATED_AT = Instant.parse("2026-10-01T08:30:00Z");
    private static final String TITLE_LENGTH_MESSAGE = "'title' must be between 3 and 255 characters";

    @Test
    void givenValidParams_whenCallWith_thenInstantiateTheReplicaAsReceived() {
        final var actualVideo = aVideo(details(EXPECTED_TITLE, EXPECTED_DESCRIPTION, Year.of(2026), 155.0, Rating.AGE_14));

        assertEquals(EXPECTED_ID, actualVideo.getId());
        assertEquals(EXPECTED_TITLE, actualVideo.getDetails().title());
        assertEquals(Year.of(2026), actualVideo.getDetails().launchedAt());
        assertEquals(155.0, actualVideo.getDetails().duration());
        assertEquals(Rating.AGE_14, actualVideo.getDetails().rating());
        assertTrue(actualVideo.getFlags().isVisibleInTheCatalog());
        assertEquals(CREATED_AT, actualVideo.getCreatedAt());
    }

    @Test
    void givenAVideo_whenCallWithCopy_thenReturnAnotherInstanceHoldingTheSameData() {
        final var video = aVideo(details(EXPECTED_TITLE, EXPECTED_DESCRIPTION, Year.of(2026), 155.0, Rating.AGE_14));

        final var actualCopy = Video.with(video);

        assertNotSame(video, actualCopy);
        assertEquals(video.getId(), actualCopy.getId());
        assertEquals(video.getDetails(), actualCopy.getDetails());
        assertEquals(video.getFlags(), actualCopy.getFlags());
        assertEquals(video.getMedias(), actualCopy.getMedias());
        assertEquals(video.getReferences(), actualCopy.getReferences());
    }

    @Test
    void givenNullDetails_whenCallWith_thenThrowNullPointerException() {
        final var actualException = assertThrows(
                NullPointerException.class,
                () -> Video.with(
                        EXPECTED_ID, null, visible(), VideoMedias.none(), VideoReferences.none(), CREATED_AT, UPDATED_AT));

        assertEquals("'details' should not be null", actualException.getMessage());
    }

    @Test
    void givenNullReferences_whenCallWith_thenThrowNullPointerException() {
        final var actualException = assertThrows(
                NullPointerException.class,
                () -> Video.with(
                        EXPECTED_ID,
                        details(EXPECTED_TITLE, EXPECTED_DESCRIPTION, Year.of(2026), 155.0, Rating.AGE_14),
                        visible(),
                        VideoMedias.none(),
                        null,
                        CREATED_AT,
                        UPDATED_AT));

        assertEquals("'references' should not be null", actualException.getMessage());
    }

    @Test
    void givenValidParams_whenCallValidate_thenAccumulateNoError() {
        final var video = aVideo(details(EXPECTED_TITLE, EXPECTED_DESCRIPTION, Year.of(2026), 155.0, Rating.AGE_14));
        final var notification = Notification.create();

        video.validate(notification);

        assertFalse(notification.hasError());
    }

    @Test
    void givenNullTitle_whenCallValidate_thenThrowDomainException() {
        final var video = aVideo(details(null, EXPECTED_DESCRIPTION, Year.of(2026), 155.0, Rating.AGE_14));

        final var actualException =
                assertThrows(DomainException.class, () -> video.validate(new ThrowsValidationHandler()));

        assertEquals("'title' should not be null", actualException.getMessage());
    }

    @Test
    void givenTitleShorterThanTheMinimum_whenCallValidate_thenThrowDomainException() {
        final var video = aVideo(details("Du", EXPECTED_DESCRIPTION, Year.of(2026), 155.0, Rating.AGE_14));

        final var actualException =
                assertThrows(DomainException.class, () -> video.validate(new ThrowsValidationHandler()));

        assertEquals(TITLE_LENGTH_MESSAGE, actualException.getMessage());
    }

    @Test
    void givenDescriptionLongerThanTheMaximum_whenCallValidate_thenThrowDomainException() {
        final var video = aVideo(details(EXPECTED_TITLE, "a".repeat(4001), Year.of(2026), 155.0, Rating.AGE_14));

        final var actualException =
                assertThrows(DomainException.class, () -> video.validate(new ThrowsValidationHandler()));

        assertEquals("'description' must be at most 4000 characters", actualException.getMessage());
    }

    @Test
    void givenNullLaunchedAt_whenCallValidate_thenThrowDomainException() {
        final var video = aVideo(details(EXPECTED_TITLE, EXPECTED_DESCRIPTION, null, 155.0, Rating.AGE_14));

        final var actualException =
                assertThrows(DomainException.class, () -> video.validate(new ThrowsValidationHandler()));

        assertEquals("'launchedAt' should not be null", actualException.getMessage());
    }

    @Test
    void givenNegativeDuration_whenCallValidate_thenThrowDomainException() {
        final var video = aVideo(details(EXPECTED_TITLE, EXPECTED_DESCRIPTION, Year.of(2026), -1.0, Rating.AGE_14));

        final var actualException =
                assertThrows(DomainException.class, () -> video.validate(new ThrowsValidationHandler()));

        assertEquals("'duration' should not be negative", actualException.getMessage());
    }

    // Rótulo de classificação desconhecido chega aqui como nulo, de propósito: o erro aparece junto
    // dos outros da mesma mensagem em vez de virar exceção na conversão.
    @Test
    void givenNullRating_whenCallValidate_thenThrowDomainException() {
        final var video = aVideo(details(EXPECTED_TITLE, EXPECTED_DESCRIPTION, Year.of(2026), 155.0, null));

        final var actualException =
                assertThrows(DomainException.class, () -> video.validate(new ThrowsValidationHandler()));

        assertEquals("'rating' should not be null", actualException.getMessage());
    }


    @Test
    void givenEverythingWrong_whenCallValidateWithNotification_thenAccumulateEveryError() {
        final var video = Video.with(
                EXPECTED_ID,
                details("  ", null, null, -1.0, null),
                visible(),
                VideoMedias.none(),
                VideoReferences.none(),
                CREATED_AT,
                UPDATED_AT);
        final var notification = Notification.create();

        video.validate(notification);

        assertEquals(5, notification.getErrors().size());
    }

    @Test
    void givenAVideoWithReferences_whenCallWith_thenHoldThem() {
        final var video = Video.with(
                EXPECTED_ID,
                details(EXPECTED_TITLE, EXPECTED_DESCRIPTION, Year.of(2026), 155.0, Rating.AGE_14),
                visible(),
                VideoMedias.none(),
                VideoReferences.with(Set.of(CategoryID.from("00000009-0000-0000-0000-000000000000")), Set.of(), Set.of()),
                CREATED_AT,
                UPDATED_AT);

        assertEquals(Set.of(CategoryID.from("00000009-0000-0000-0000-000000000000")), video.getReferences().categories());
    }

    private static VideoDetails details(
            final String title, final String description, final Year launchedAt, final double duration, final Rating rating) {
        return VideoDetails.with(title, description, launchedAt, duration, rating);
    }

    private static VideoFlags visible() {
        return new VideoFlags(false, true, true);
    }

    private static Video aVideo(final VideoDetails details) {
        return Video.with(
                EXPECTED_ID, details, visible(), VideoMedias.none(), VideoReferences.none(), CREATED_AT, UPDATED_AT);
    }
}
