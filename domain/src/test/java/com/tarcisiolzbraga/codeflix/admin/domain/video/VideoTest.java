package com.tarcisiolzbraga.codeflix.admin.domain.video;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tarcisiolzbraga.codeflix.admin.domain.category.CategoryID;
import com.tarcisiolzbraga.codeflix.admin.domain.exceptions.DomainException;
import com.tarcisiolzbraga.codeflix.admin.domain.validation.ValidationError;
import com.tarcisiolzbraga.codeflix.admin.domain.validation.handler.Notification;
import com.tarcisiolzbraga.codeflix.admin.domain.validation.handler.ThrowsValidationHandler;
import java.time.Year;
import java.util.Optional;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class VideoTest {

    private static final String EXPECTED_TITLE = "Duna";
    private static final String EXPECTED_DESCRIPTION = "Paul Atreides em Arrakis";
    private static final Year EXPECTED_YEAR = Year.of(2021);
    private static final double EXPECTED_DURATION = 155.0;
    private static final String TITLE_LENGTH_MESSAGE = "'title' must be between 1 and 255 characters";

    @Test
    void givenValidParams_whenCallNewVideo_thenInstantiateItClosedAndUnpublished() {
        final var actualVideo = newVideo();

        assertNotNull(actualVideo.getId());
        assertEquals(EXPECTED_TITLE, actualVideo.getTitle());
        assertEquals(EXPECTED_DESCRIPTION, actualVideo.getDescription());
        assertEquals(EXPECTED_YEAR, actualVideo.getLaunchedAt());
        assertEquals(EXPECTED_DURATION, actualVideo.getDuration());
        assertEquals(Rating.AGE_12, actualVideo.getRating());
        assertTrue(actualVideo.isActive());
        assertFalse(actualVideo.isOpened());
        assertFalse(actualVideo.isPublished());
        assertEquals(actualVideo.getCreatedAt(), actualVideo.getUpdatedAt());
    }

    @Test
    void givenReferences_whenCallNewVideo_thenBeBornWithThem() {
        final var category = CategoryID.unique();

        final var actualVideo = Video.newVideo(
                details(EXPECTED_TITLE), VideoReferences.with(Set.of(category), Set.of(), Set.of()));

        assertEquals(Set.of(category), actualVideo.getCategories());
        assertEquals(actualVideo.getCreatedAt(), actualVideo.getUpdatedAt());
    }

    @Test
    void givenNullTitle_whenCallValidate_thenReceiveAnError() {
        final var actualVideo = Video.newVideo(details(null), VideoReferences.none());

        final var actualException =
                assertThrows(DomainException.class, () -> actualVideo.validate(new ThrowsValidationHandler()));

        assertEquals("'title' should not be null", actualException.getErrors().getFirst().message());
    }

    @Test
    void givenEmptyTitle_whenCallValidate_thenReceiveAnError() {
        final var actualVideo = Video.newVideo(details("  "), VideoReferences.none());

        final var actualException =
                assertThrows(DomainException.class, () -> actualVideo.validate(new ThrowsValidationHandler()));

        assertEquals("'title' should not be empty", actualException.getErrors().getFirst().message());
    }

    @Test
    void givenTitleLongerThanTwoHundredFiftyFiveCharacters_whenCallValidate_thenReceiveAnError() {
        final var actualVideo = Video.newVideo(details("a".repeat(256)), VideoReferences.none());

        final var actualException =
                assertThrows(DomainException.class, () -> actualVideo.validate(new ThrowsValidationHandler()));

        assertEquals(TITLE_LENGTH_MESSAGE, actualException.getErrors().getFirst().message());
    }

    @Test
    void givenDescriptionLongerThanFourThousandCharacters_whenCallValidate_thenReceiveAnError() {
        final var actualVideo = Video.newVideo(
                VideoDetails.with(EXPECTED_TITLE, "a".repeat(4001), EXPECTED_YEAR, EXPECTED_DURATION, Rating.AGE_12),
                VideoReferences.none());

        final var actualException =
                assertThrows(DomainException.class, () -> actualVideo.validate(new ThrowsValidationHandler()));

        assertEquals(
                "'description' must be between 1 and 4000 characters",
                actualException.getErrors().getFirst().message());
    }

    @Test
    void givenNegativeDuration_whenCallValidate_thenReceiveAnError() {
        final var actualVideo = Video.newVideo(
                VideoDetails.with(EXPECTED_TITLE, EXPECTED_DESCRIPTION, EXPECTED_YEAR, -1.0, Rating.AGE_12),
                VideoReferences.none());

        final var actualException =
                assertThrows(DomainException.class, () -> actualVideo.validate(new ThrowsValidationHandler()));

        assertEquals("'duration' should not be negative", actualException.getErrors().getFirst().message());
    }

    @Test
    void givenNullTitleAndNullLaunchedAtAndNullRating_whenCallValidate_thenAccumulateEveryError() {
        final var actualVideo = Video.newVideo(
                VideoDetails.with(null, EXPECTED_DESCRIPTION, null, EXPECTED_DURATION, null),
                VideoReferences.none());
        final var notification = Notification.create();

        actualVideo.validate(notification);

        assertEquals(
                List.of("'title' should not be null", "'launchedAt' should not be null", "'rating' should not be null"),
                notification.getErrors().stream().map(ValidationError::message).toList());
    }

    @Test
    void givenValidParams_whenCallUpdate_thenReplaceDetailsAndReferencesAndRefreshUpdatedAt() {
        final var actualVideo = newVideo();
        final var updatedAt = actualVideo.getUpdatedAt();
        final var category = CategoryID.unique();

        actualVideo.update(
                VideoDetails.with("Duna 2", EXPECTED_DESCRIPTION, Year.of(2024), 166.0, Rating.AGE_14),
                VideoReferences.with(Set.of(category), Set.of(), Set.of()));

        assertEquals("Duna 2", actualVideo.getTitle());
        assertEquals(Rating.AGE_14, actualVideo.getRating());
        assertEquals(Set.of(category), actualVideo.getCategories());
        assertTrue(updatedAt.isBefore(actualVideo.getUpdatedAt()));
    }

    @Test
    void givenUnpublishedVideo_whenCallPublish_thenTurnItPublished() {
        final var actualVideo = newVideo();
        final var updatedAt = actualVideo.getUpdatedAt();

        actualVideo.publish();

        assertTrue(actualVideo.isPublished());
        assertTrue(updatedAt.isBefore(actualVideo.getUpdatedAt()));
    }

    @Test
    void givenPublishedVideo_whenCallUnpublish_thenTurnItUnpublished() {
        final var actualVideo = newVideo();
        actualVideo.publish();

        actualVideo.unpublish();

        assertFalse(actualVideo.isPublished());
    }

    @Test
    void givenClosedVideo_whenCallOpen_thenTurnItOpened() {
        final var actualVideo = newVideo();

        actualVideo.open();

        assertTrue(actualVideo.isOpened());
    }

    @Test
    void givenOpenedVideo_whenCallClose_thenTurnItClosed() {
        final var actualVideo = newVideo();
        actualVideo.open();

        actualVideo.close();

        assertFalse(actualVideo.isOpened());
    }

    @Test
    void givenPersistedValues_whenCallWith_thenRebuildTheSameVideo() {
        final var expectedVideo = newVideo();

        final var actualVideo = Video.with(
                expectedVideo.getId(),
                details(EXPECTED_TITLE),
                VideoReferences.none(),
                VideoMedias.none(),
                new VideoFlags(true, true, false),
                expectedVideo.getCreatedAt(),
                expectedVideo.getUpdatedAt());

        assertEquals(expectedVideo, actualVideo);
        assertTrue(actualVideo.isOpened());
        assertTrue(actualVideo.isPublished());
        assertFalse(actualVideo.isActive());
    }

    @Test
    void givenValidParams_whenCallNewVideo_thenBeBornWithoutMedias() {
        final var actualVideo = newVideo();

        assertTrue(actualVideo.getVideo().isEmpty());
        assertTrue(actualVideo.getTrailer().isEmpty());
        assertTrue(actualVideo.getBanner().isEmpty());
        assertTrue(actualVideo.getThumbnail().isEmpty());
        assertTrue(actualVideo.getThumbnailHalf().isEmpty());
    }

    @Test
    void givenAVideo_whenCallUpdateVideoMedia_thenKeepItAndRefreshUpdatedAt() {
        final var actualVideo = newVideo();
        final var updatedAt = actualVideo.getUpdatedAt();
        final var expectedMedia = audioVideoMedia();

        actualVideo.updateVideoMedia(expectedMedia);

        assertEquals(Optional.of(expectedMedia), actualVideo.getVideo());
        assertTrue(updatedAt.isBefore(actualVideo.getUpdatedAt()));
    }

    @Test
    void givenAVideo_whenCallUpdateTrailerMedia_thenKeepItAndRefreshUpdatedAt() {
        final var actualVideo = newVideo();
        final var updatedAt = actualVideo.getUpdatedAt();
        final var expectedMedia = audioVideoMedia();

        actualVideo.updateTrailerMedia(expectedMedia);

        assertEquals(Optional.of(expectedMedia), actualVideo.getTrailer());
        assertTrue(updatedAt.isBefore(actualVideo.getUpdatedAt()));
    }

    @Test
    void givenAVideo_whenCallUpdateBanner_thenKeepItAndRefreshUpdatedAt() {
        final var actualVideo = newVideo();
        final var updatedAt = actualVideo.getUpdatedAt();
        final var expectedMedia = imageMedia();

        actualVideo.updateBanner(expectedMedia);

        assertEquals(Optional.of(expectedMedia), actualVideo.getBanner());
        assertTrue(updatedAt.isBefore(actualVideo.getUpdatedAt()));
    }

    @Test
    void givenAVideo_whenCallUpdateThumbnail_thenKeepItAndRefreshUpdatedAt() {
        final var actualVideo = newVideo();
        final var updatedAt = actualVideo.getUpdatedAt();
        final var expectedMedia = imageMedia();

        actualVideo.updateThumbnail(expectedMedia);

        assertEquals(Optional.of(expectedMedia), actualVideo.getThumbnail());
        assertTrue(updatedAt.isBefore(actualVideo.getUpdatedAt()));
    }

    @Test
    void givenAVideo_whenCallUpdateThumbnailHalf_thenKeepItAndRefreshUpdatedAt() {
        final var actualVideo = newVideo();
        final var updatedAt = actualVideo.getUpdatedAt();
        final var expectedMedia = imageMedia();

        actualVideo.updateThumbnailHalf(expectedMedia);

        assertEquals(Optional.of(expectedMedia), actualVideo.getThumbnailHalf());
        assertTrue(updatedAt.isBefore(actualVideo.getUpdatedAt()));
    }

    @Test
    void givenPersistedMedias_whenCallWith_thenRebuildThem() {
        final var expectedVideo = audioVideoMedia();
        final var expectedBanner = imageMedia();
        final var medias = VideoMedias.with(expectedVideo, null, expectedBanner, null, null);

        final var actualVideo = rebuild(medias);

        assertEquals(Optional.of(expectedVideo), actualVideo.getVideo());
        assertEquals(Optional.of(expectedBanner), actualVideo.getBanner());
        assertTrue(actualVideo.getTrailer().isEmpty());
    }

    @Test
    void givenNullMedia_whenCallUpdateVideoMedia_thenReceiveAnError() {
        final var actualVideo = newVideo();

        final var actualException =
                assertThrows(NullPointerException.class, () -> actualVideo.updateVideoMedia(null));

        assertEquals("'media' should not be null", actualException.getMessage());
    }

    @Test
    void givenNullMedia_whenCallUpdateBanner_thenReceiveAnError() {
        final var actualVideo = newVideo();

        final var actualException = assertThrows(NullPointerException.class, () -> actualVideo.updateBanner(null));

        assertEquals("'media' should not be null", actualException.getMessage());
    }

    @Test
    void givenAVideo_whenCallUpdateVideoMedia_thenRegisterTheMediaCreatedEvent() {
        final var actualVideo = newVideo();

        actualVideo.updateVideoMedia(audioVideoMedia());

        final var actualEvent = (VideoMediaCreated) actualVideo.getDomainEvents().getFirst();
        assertEquals(actualVideo.getId().getValue(), actualEvent.videoId());
        assertEquals(VideoMediaType.VIDEO, actualEvent.type());
        assertEquals("videoId-VIDEO", actualEvent.filePath());
        assertNotNull(actualEvent.occurredOn());
    }

    @Test
    void givenAVideo_whenCallUpdateTrailerMedia_thenRegisterTheEventForTheTrailer() {
        final var actualVideo = newVideo();

        actualVideo.updateTrailerMedia(audioVideoMedia());

        final var actualEvent = (VideoMediaCreated) actualVideo.getDomainEvents().getFirst();
        assertEquals(VideoMediaType.TRAILER, actualEvent.type());
    }

    @Test
    void givenAVideo_whenCallUpdateBanner_thenRegisterNoEvent() {
        final var actualVideo = newVideo();

        actualVideo.updateBanner(imageMedia());

        assertTrue(actualVideo.getDomainEvents().isEmpty());
    }

    private Video rebuild(final VideoMedias medias) {
        final var video = newVideo();
        return Video.with(
                video.getId(),
                details(EXPECTED_TITLE),
                VideoReferences.none(),
                medias,
                new VideoFlags(false, false, true),
                video.getCreatedAt(),
                video.getUpdatedAt());
    }

    private AudioVideoMedia audioVideoMedia() {
        return AudioVideoMedia.with("abc123", "duna.mp4", "videoId-VIDEO");
    }

    private ImageMedia imageMedia() {
        return ImageMedia.with("abc123", "duna.png", "videoId-BANNER");
    }

    private Video newVideo() {
        return Video.newVideo(details(EXPECTED_TITLE), VideoReferences.none());
    }

    private VideoDetails details(final String title) {
        return VideoDetails.with(title, EXPECTED_DESCRIPTION, EXPECTED_YEAR, EXPECTED_DURATION, Rating.AGE_12);
    }
}
