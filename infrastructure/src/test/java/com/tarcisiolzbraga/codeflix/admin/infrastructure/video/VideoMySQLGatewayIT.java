package com.tarcisiolzbraga.codeflix.admin.infrastructure.video;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tarcisiolzbraga.codeflix.admin.domain.castmember.CastMember;
import com.tarcisiolzbraga.codeflix.admin.domain.castmember.CastMemberGateway;
import com.tarcisiolzbraga.codeflix.admin.domain.castmember.CastMemberID;
import com.tarcisiolzbraga.codeflix.admin.domain.castmember.CastMemberType;
import com.tarcisiolzbraga.codeflix.admin.domain.category.Category;
import com.tarcisiolzbraga.codeflix.admin.domain.category.CategoryGateway;
import com.tarcisiolzbraga.codeflix.admin.domain.category.CategoryID;
import com.tarcisiolzbraga.codeflix.admin.domain.genre.Genre;
import com.tarcisiolzbraga.codeflix.admin.domain.genre.GenreGateway;
import com.tarcisiolzbraga.codeflix.admin.domain.pagination.SearchQuery;
import com.tarcisiolzbraga.codeflix.admin.domain.video.Rating;
import com.tarcisiolzbraga.codeflix.admin.domain.video.Video;
import com.tarcisiolzbraga.codeflix.admin.domain.video.VideoDetails;
import com.tarcisiolzbraga.codeflix.admin.domain.video.VideoID;
import com.tarcisiolzbraga.codeflix.admin.domain.video.VideoReferences;
import com.tarcisiolzbraga.codeflix.admin.domain.video.VideoSearchQuery;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.IntegrationTest;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.video.persistence.VideoRepository;
import java.time.Year;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@IntegrationTest
class VideoMySQLGatewayIT {

    private static final String DUNA = "Duna";
    private static final String MATRIX = "Matrix";
    private static final SearchQuery BY_TITLE = new SearchQuery(0, 10, null, "title", "asc");

    @Autowired
    private VideoMySQLGateway gateway;

    @Autowired
    private VideoRepository videoRepository;

    @Autowired
    private CategoryGateway categoryGateway;

    @Autowired
    private GenreGateway genreGateway;

    @Autowired
    private CastMemberGateway castMemberGateway;

    @Test
    void givenVideoWithReferences_whenCallCreate_thenPersistIt() {
        final var category = existingCategory("Filmes");
        final var video = Video.newVideo(details(DUNA), VideoReferences.with(Set.of(category), Set.of(), Set.of()));

        final var actualVideo = this.gateway.create(video);

        assertEquals(video.getId(), actualVideo.getId());
        assertEquals(1, this.videoRepository.count());
        assertEquals(Set.of(category), reload(video).getCategories());
    }

    @Test
    void givenPersistedVideo_whenCallUpdate_thenReplaceTitleAndReferences() {
        final var category = existingCategory("Filmes");
        final var video = this.gateway.create(Video.newVideo(details(DUNA), VideoReferences.none()));

        this.gateway.update(video.update(details(MATRIX), VideoReferences.with(Set.of(category), Set.of(), Set.of())));

        final var actualVideo = reload(video);
        assertEquals(MATRIX, actualVideo.getTitle());
        assertEquals(Set.of(category), actualVideo.getCategories());
    }

    @Test
    void givenPersistedVideo_whenCallDeleteById_thenRemoveItAndKeepTheReferences() {
        final var category = existingCategory("Filmes");
        final var video = this.gateway.create(
                Video.newVideo(details(DUNA), VideoReferences.with(Set.of(category), Set.of(), Set.of())));

        this.gateway.deleteById(video.getId());

        assertEquals(0, this.videoRepository.count());
        assertTrue(this.categoryGateway.findById(category).isPresent());
    }

    @Test
    void givenUnknownId_whenCallDeleteById_thenDoNothing() {
        this.gateway.create(Video.newVideo(details(DUNA), VideoReferences.none()));

        this.gateway.deleteById(VideoID.unique());

        assertEquals(1, this.videoRepository.count());
    }

    @Test
    void givenUnknownId_whenCallFindById_thenReturnEmpty() {
        assertTrue(this.gateway.findById(VideoID.unique()).isEmpty());
    }

    @Test
    void givenPersistedVideos_whenCallFindAll_thenReturnPaginatedByTitle() {
        this.gateway.create(Video.newVideo(details(MATRIX), VideoReferences.none()));
        this.gateway.create(Video.newVideo(details(DUNA), VideoReferences.none()));

        final var actualPage = this.gateway.findAll(VideoSearchQuery.with(BY_TITLE));

        assertEquals(2, actualPage.total());
        assertEquals(DUNA, actualPage.items().getFirst().getTitle());
        assertEquals(MATRIX, actualPage.items().getLast().getTitle());
    }

    @Test
    void givenTerms_whenCallFindAll_thenFilterByTitleIgnoringCase() {
        this.gateway.create(Video.newVideo(details(MATRIX), VideoReferences.none()));
        this.gateway.create(Video.newVideo(details(DUNA), VideoReferences.none()));

        final var actualPage =
                this.gateway.findAll(VideoSearchQuery.with(new SearchQuery(0, 10, "matr", "title", "asc")));

        assertEquals(1, actualPage.total());
        assertEquals(MATRIX, actualPage.items().getFirst().getTitle());
    }

    @Test
    void givenACategoryFilter_whenCallFindAll_thenReturnOnlyTheVideosLinkedToIt() {
        final var movies = existingCategory("Filmes");
        final var series = existingCategory("Séries");
        this.gateway.create(Video.newVideo(details(DUNA), VideoReferences.with(Set.of(movies), Set.of(), Set.of())));
        this.gateway.create(Video.newVideo(details(MATRIX), VideoReferences.with(Set.of(series), Set.of(), Set.of())));

        final var actualPage =
                this.gateway.findAll(new VideoSearchQuery(BY_TITLE, Set.of(movies), Set.of(), Set.of()));

        assertEquals(1, actualPage.total());
        assertEquals(DUNA, actualPage.items().getFirst().getTitle());
    }

    @Test
    void givenAGenreAndAMemberFilter_whenCallFindAll_thenReturnOnlyTheVideoWithBoth() {
        final var fiction = this.genreGateway.create(Genre.newGenre("Ficção", true)).getId();
        final var actor = existingCastMember();
        this.gateway.create(Video.newVideo(
                details(DUNA), VideoReferences.with(Set.of(), Set.of(fiction), Set.of(actor))));
        this.gateway.create(Video.newVideo(details(MATRIX), VideoReferences.none()));

        final var actualPage = this.gateway.findAll(
                new VideoSearchQuery(BY_TITLE, Set.of(), Set.of(fiction), Set.of(actor)));

        assertEquals(1, actualPage.total());
        assertEquals(DUNA, actualPage.items().getFirst().getTitle());
    }

    @Test
    void givenNoVideo_whenCallFindAll_thenReturnEmptyPage() {
        final var actualPage = this.gateway.findAll(VideoSearchQuery.with(BY_TITLE));

        assertEquals(0, actualPage.total());
        assertTrue(actualPage.items().isEmpty());
    }

    @Test
    void givenVideoLinkedToACategory_whenCallExistsByCategory_thenReturnTrue() {
        final var category = existingCategory("Filmes");
        this.gateway.create(Video.newVideo(details(DUNA), VideoReferences.with(Set.of(category), Set.of(), Set.of())));

        assertTrue(this.gateway.existsByCategory(category));
    }

    @Test
    void givenCategoryWithoutVideos_whenCallExistsByCategory_thenReturnFalse() {
        final var category = existingCategory("Filmes");
        this.gateway.create(Video.newVideo(details(DUNA), VideoReferences.none()));

        assertFalse(this.gateway.existsByCategory(category));
    }

    @Test
    void givenVideoLinkedToAGenre_whenCallExistsByGenre_thenReturnTrue() {
        final var fiction = this.genreGateway.create(Genre.newGenre("Ficção", true)).getId();
        this.gateway.create(Video.newVideo(details(DUNA), VideoReferences.with(Set.of(), Set.of(fiction), Set.of())));

        assertTrue(this.gateway.existsByGenre(fiction));
    }

    @Test
    void givenVideoLinkedToACastMember_whenCallExistsByCastMember_thenReturnTrue() {
        final var actor = existingCastMember();
        this.gateway.create(Video.newVideo(details(DUNA), VideoReferences.with(Set.of(), Set.of(), Set.of(actor))));

        assertTrue(this.gateway.existsByCastMember(actor));
    }

    private VideoDetails details(final String title) {
        return VideoDetails.with(title, "Uma descrição", Year.of(2021), 155.0, Rating.AGE_12);
    }

    private CategoryID existingCategory(final String name) {
        return this.categoryGateway.create(Category.newCategory(name, null, true)).getId();
    }

    private CastMemberID existingCastMember() {
        return this.castMemberGateway
                .create(CastMember.newCastMember("Timothée Chalamet", CastMemberType.ACTOR, true))
                .getId();
    }

    private Video reload(final Video video) {
        return this.gateway.findById(video.getId()).orElseThrow();
    }
}
