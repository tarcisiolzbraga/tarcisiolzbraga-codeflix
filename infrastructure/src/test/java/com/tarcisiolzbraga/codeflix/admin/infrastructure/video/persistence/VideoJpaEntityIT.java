package com.tarcisiolzbraga.codeflix.admin.infrastructure.video.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tarcisiolzbraga.codeflix.admin.domain.castmember.CastMember;
import com.tarcisiolzbraga.codeflix.admin.domain.castmember.CastMemberGateway;
import com.tarcisiolzbraga.codeflix.admin.domain.castmember.CastMemberType;
import com.tarcisiolzbraga.codeflix.admin.domain.category.Category;
import com.tarcisiolzbraga.codeflix.admin.domain.category.CategoryGateway;
import com.tarcisiolzbraga.codeflix.admin.domain.category.CategoryID;
import com.tarcisiolzbraga.codeflix.admin.domain.genre.Genre;
import com.tarcisiolzbraga.codeflix.admin.domain.genre.GenreGateway;
import com.tarcisiolzbraga.codeflix.admin.domain.video.Rating;
import com.tarcisiolzbraga.codeflix.admin.domain.video.Video;
import com.tarcisiolzbraga.codeflix.admin.domain.video.VideoDetails;
import com.tarcisiolzbraga.codeflix.admin.domain.video.VideoFixture;
import com.tarcisiolzbraga.codeflix.admin.domain.video.VideoReferences;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.IntegrationTest;
import java.time.Year;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

@IntegrationTest
class VideoJpaEntityIT {

    private static final String EXPECTED_TITLE = "Duna";

    @Autowired
    private VideoRepository videoRepository;

    @Autowired
    private CategoryGateway categoryGateway;

    @Autowired
    private GenreGateway genreGateway;

    @Autowired
    private CastMemberGateway castMemberGateway;

    @Test
    void givenVideoWithTheThreeReferences_whenSaveAndReload_thenKeepAllFields() {
        final var references = existingReferences();
        final var video = Video.newVideo(details(), references);

        final var actualVideo = saveAndReload(video);

        assertEquals(video.getId(), actualVideo.getId());
        assertEquals(EXPECTED_TITLE, actualVideo.getTitle());
        assertEquals(Year.of(2021), actualVideo.getLaunchedAt());
        assertEquals(155.0, actualVideo.getDuration());
        assertEquals(Rating.AGE_12, actualVideo.getRating());
        assertEquals(references.categories(), actualVideo.getCategories());
        assertEquals(references.genres(), actualVideo.getGenres());
        assertEquals(references.castMembers(), actualVideo.getCastMembers());
    }

    @Test
    void givenNewVideo_whenSaveAndReload_thenItIsActiveClosedAndUnpublished() {
        final var actualVideo = saveAndReload(Video.newVideo(details(), VideoReferences.none()));

        assertTrue(actualVideo.isActive());
        assertFalse(actualVideo.isOpened());
        assertFalse(actualVideo.isPublished());
        assertTrue(actualVideo.getCategories().isEmpty());
    }

    @Test
    void givenPublishedAndOpenedVideo_whenSaveAndReload_thenKeepBothStates() {
        final var video = Video.newVideo(details(), VideoReferences.none());
        video.publish();
        video.open();

        final var actualVideo = saveAndReload(video);

        assertTrue(actualVideo.isPublished());
        assertTrue(actualVideo.isOpened());
    }

    @Test
    void givenSavedVideo_whenSaveWithoutOneCategory_thenRemoveOnlyThatLink() {
        final var references = existingReferences();
        final var video = saveAndReload(Video.newVideo(details(), references));
        final var keptCategory = references.categories().iterator().next();

        final var actualVideo = saveAndReload(video.update(
                details(),
                VideoReferences.with(Set.of(keptCategory), references.genres(), references.castMembers())));

        assertEquals(Set.of(keptCategory), actualVideo.getCategories());
        assertEquals(references.genres(), actualVideo.getGenres());
    }

    @Test
    void givenVideoReferencingAnUnknownCategory_whenSave_thenFailByTheForeignKey() {
        final var video = Video.newVideo(
                details(), VideoReferences.with(Set.of(CategoryID.unique()), Set.of(), Set.of()));
        final var entity = VideoJpaEntity.from(video);

        assertThrows(DataIntegrityViolationException.class, () -> this.videoRepository.saveAndFlush(entity));
    }

    private VideoDetails details() {
        return VideoFixture.details();
    }

    private VideoReferences existingReferences() {
        final var movies = this.categoryGateway.create(Category.newCategory("Filmes", null, true)).getId();
        final var series = this.categoryGateway.create(Category.newCategory("Séries", null, true)).getId();
        final var fiction = this.genreGateway.create(Genre.newGenre("Ficção", true)).getId();
        final var actor = this.castMemberGateway
                .create(CastMember.newCastMember("Timothée Chalamet", CastMemberType.ACTOR, true))
                .getId();
        return VideoReferences.with(setOf(movies, series), setOf(fiction), setOf(actor));
    }

    @SafeVarargs
    private <T> Set<T> setOf(final T... values) {
        return Set.of(values);
    }

    private Video saveAndReload(final Video video) {
        this.videoRepository.saveAndFlush(VideoJpaEntity.from(video));
        return this.videoRepository
                .findById(video.getId().getValue())
                .orElseThrow()
                .toAggregate();
    }
}
