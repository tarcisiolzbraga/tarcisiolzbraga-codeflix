package com.tarcisiolzbraga.codeflix.videos.infrastructure.video;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tarcisiolzbraga.codeflix.videos.domain.castmember.CastMemberID;
import com.tarcisiolzbraga.codeflix.videos.domain.category.CategoryID;
import com.tarcisiolzbraga.codeflix.videos.domain.genre.GenreID;
import com.tarcisiolzbraga.codeflix.videos.domain.pagination.Pagination;
import com.tarcisiolzbraga.codeflix.videos.domain.video.Rating;
import com.tarcisiolzbraga.codeflix.videos.domain.video.Video;
import com.tarcisiolzbraga.codeflix.videos.domain.video.VideoDetails;
import com.tarcisiolzbraga.codeflix.videos.domain.video.VideoFlags;
import com.tarcisiolzbraga.codeflix.videos.domain.video.VideoID;
import com.tarcisiolzbraga.codeflix.videos.domain.video.VideoMedias;
import com.tarcisiolzbraga.codeflix.videos.domain.video.VideoReferences;
import com.tarcisiolzbraga.codeflix.videos.domain.video.VideoSearchQuery;
import com.tarcisiolzbraga.codeflix.videos.infrastructure.IntegrationTest;
import com.tarcisiolzbraga.codeflix.videos.infrastructure.video.persistence.VideoDocument;
import com.tarcisiolzbraga.codeflix.videos.infrastructure.video.persistence.VideoRepository;
import java.time.Instant;
import java.time.Year;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;

@IntegrationTest
class VideoElasticsearchGatewayIT {

    private static final Instant CREATED_AT = Instant.parse("2026-09-30T12:00:00Z");
    private static final Instant UPDATED_AT = Instant.parse("2026-10-01T08:30:00Z");
    private static final CategoryID FILMES = CategoryID.from("00000005-0000-0000-0000-000000000000");
    private static final GenreID ACAO = GenreID.from("00000012-0000-0000-0000-000000000000");
    private static final CastMemberID DENIS = CastMemberID.from("00000019-0000-0000-0000-000000000000");

    @Autowired
    private VideoElasticsearchGateway gateway;

    @Autowired
    private ElasticsearchOperations operations;

    @Autowired
    private VideoRepository repository;

    // Este é o teste que mais importa do documento: ele volta inteiro do índice, com todos os campos
    // e as três relações, ou algum fica nulo pelo caminho.
    @Test
    void givenAVideo_whenCallSave_thenReadItBackWholeIncludingTheThreeRelations() {
        final var video = aVideo("00000001-0000-0000-0000-000000000000", "Duna", Rating.AGE_14, 2026, true, true, references());

        gateway.save(video);

        final var stored = gateway.findById(VideoID.from("00000001-0000-0000-0000-000000000000")).orElseThrow();
        assertEquals("Duna", stored.getDetails().title());
        assertEquals("Paul Atreides em Arrakis", stored.getDetails().description());
        assertEquals(Year.of(2026), stored.getDetails().launchedAt());
        assertEquals(155.0, stored.getDetails().duration());
        assertEquals(Rating.AGE_14, stored.getDetails().rating());
        assertTrue(stored.getFlags().published());
        assertFalse(stored.getFlags().opened());
        assertEquals("v.mp4", stored.getMedias().video());
        assertEquals("thh.jpg", stored.getMedias().thumbnailHalf());
        assertEquals(Set.of(FILMES), stored.getReferences().categories());
        assertEquals(Set.of(ACAO), stored.getReferences().genres());
        assertEquals(Set.of(DENIS), stored.getReferences().castMembers());
        assertEquals(CREATED_AT, stored.getCreatedAt());
        assertEquals(UPDATED_AT, stored.getUpdatedAt());
    }

    @Test
    void givenAVideoWithoutMediaOrRelations_whenCallSave_thenReadItBackEmptyNotNull() {
        gateway.save(Video.with(
                VideoID.from("00000001-0000-0000-0000-000000000000"),
                details("Duna", Rating.AGE_14, 2026),
                new VideoFlags(false, true, true),
                VideoMedias.none(),
                VideoReferences.none(),
                CREATED_AT,
                UPDATED_AT));

        final var stored = gateway.findById(VideoID.from("00000001-0000-0000-0000-000000000000")).orElseThrow();

        assertTrue(stored.getReferences().categories().isEmpty());
        assertTrue(stored.getReferences().genres().isEmpty());
    }

    @Test
    void givenAnInactiveVideo_whenCallFindById_thenReturnEmptyButKeepItStored() {
        gateway.save(aVideo("00000001-0000-0000-0000-000000000000", "Duna", Rating.AGE_14, 2026, false, true, references()));

        assertTrue(gateway.findById(VideoID.from("00000001-0000-0000-0000-000000000000")).isEmpty());
        assertTrue(repository.findById(UUID.fromString("00000001-0000-0000-0000-000000000000")).isPresent());
    }

    // A metade que o curso também filtra: não publicado não é servido.
    @Test
    void givenAnUnpublishedVideo_whenCallFindById_thenReturnEmptyButKeepItStored() {
        gateway.save(aVideo("00000001-0000-0000-0000-000000000000", "Duna", Rating.AGE_14, 2026, true, false, references()));

        assertTrue(gateway.findById(VideoID.from("00000001-0000-0000-0000-000000000000")).isEmpty());
        assertTrue(repository.findById(UUID.fromString("00000001-0000-0000-0000-000000000000")).isPresent());
    }

    @Test
    void givenAStoredVideo_whenCallDeleteById_thenRemoveIt() {
        gateway.save(aVideo("00000001-0000-0000-0000-000000000000", "Duna", Rating.AGE_14, 2026, true, true, references()));

        gateway.deleteById(VideoID.from("00000001-0000-0000-0000-000000000000"));

        assertTrue(repository.findById(UUID.fromString("00000001-0000-0000-0000-000000000000")).isEmpty());
    }

    @Test
    void givenStoredVideos_whenCallFindAllWithoutFilters_thenReturnThePageSortedByTitle() {
        seed();

        final var actualPage = gateway.findAll(aQuery(null, null, null, Set.of(), Set.of(), Set.of(), 0, 10));

        assertEquals(3L, actualPage.total());
        assertEquals(List.of("Arrival", "Duna", "Sicario"), titlesOf(actualPage));
    }

    @Test
    void givenAnInactiveOrUnpublishedVideo_whenCallFindAll_thenHideBoth() {
        gateway.save(aVideo("00000001-0000-0000-0000-000000000000", "Duna", Rating.AGE_14, 2026, true, true, references()));
        gateway.save(aVideo("00000002-0000-0000-0000-000000000000", "Inativo", Rating.AGE_14, 2026, false, true, references()));
        gateway.save(aVideo("00000003-0000-0000-0000-000000000000", "Nao publicado", Rating.AGE_14, 2026, true, false, references()));
        refresh();

        final var actualPage = gateway.findAll(aQuery(null, null, null, Set.of(), Set.of(), Set.of(), 0, 10));

        assertEquals(1L, actualPage.total());
        assertEquals(List.of("Duna"), titlesOf(actualPage));
    }

    @Test
    void givenTermsMatchingTheTitle_whenCallFindAll_thenReturnOnlyWhatMatches() {
        seed();

        final var actualPage = gateway.findAll(aQuery("Sicario", null, null, Set.of(), Set.of(), Set.of(), 0, 10));

        assertEquals(List.of("Sicario"), titlesOf(actualPage));
    }

    @Test
    void givenARating_whenCallFindAll_thenReturnOnlyTheVideosWithIt() {
        seed();

        final var actualPage = gateway.findAll(aQuery(null, Rating.AGE_16, null, Set.of(), Set.of(), Set.of(), 0, 10));

        assertEquals(List.of("Sicario"), titlesOf(actualPage));
    }

    @Test
    void givenAYear_whenCallFindAll_thenReturnOnlyTheVideosLaunchedThen() {
        seed();

        final var actualPage = gateway.findAll(aQuery(null, null, 2016, Set.of(), Set.of(), Set.of(), 0, 10));

        assertEquals(List.of("Arrival"), titlesOf(actualPage));
    }

    @Test
    void givenACategory_whenCallFindAll_thenReturnOnlyTheVideosLinkedToIt() {
        seed();

        final var actualPage =
                gateway.findAll(aQuery(null, null, null, Set.of(CategoryID.from("00000008-0000-0000-0000-000000000000")), Set.of(), Set.of(), 0, 10));

        assertEquals(List.of("Arrival"), titlesOf(actualPage));
    }

    @Test
    void givenAGenre_whenCallFindAll_thenReturnOnlyTheVideosLinkedToIt() {
        seed();

        final var actualPage =
                gateway.findAll(aQuery(null, null, null, Set.of(), Set.of(GenreID.from("00000014-0000-0000-0000-000000000000")), Set.of(), 0, 10));

        assertEquals(List.of("Sicario"), titlesOf(actualPage));
    }

    @Test
    void givenACastMember_whenCallFindAll_thenReturnOnlyTheVideosLinkedToIt() {
        seed();

        final var actualPage =
                gateway.findAll(aQuery(null, null, null, Set.of(), Set.of(), Set.of(DENIS), 0, 10));

        assertEquals(3L, actualPage.total());
    }

    // Os cinco filtros juntos: é o caso que prova que cada um entra agrupado e nenhum anula o outro.
    @Test
    void givenEveryFilterAtOnce_whenCallFindAll_thenApplyAllOfThem() {
        seed();

        final var actualPage = gateway.findAll(
                aQuery("Duna", Rating.AGE_14, 2026, Set.of(FILMES), Set.of(ACAO), Set.of(DENIS), 0, 10));

        assertEquals(1L, actualPage.total());
        assertEquals(List.of("Duna"), titlesOf(actualPage));
    }

    @Test
    void givenFiltersThatMatchNothingTogether_whenCallFindAll_thenReturnAnEmptyPage() {
        seed();

        final var actualPage =
                gateway.findAll(aQuery("Duna", Rating.AGE_16, null, Set.of(), Set.of(), Set.of(), 0, 10));

        assertEquals(0L, actualPage.total());
    }

    @Test
    void givenASecondPage_whenCallFindAll_thenReturnOnlyItsItemsAndTheFullTotal() {
        seed();

        final var actualPage = gateway.findAll(aQuery(null, null, null, Set.of(), Set.of(), Set.of(), 1, 2));

        assertEquals(1, actualPage.currentPage());
        assertEquals(3L, actualPage.total());
        assertEquals(List.of("Sicario"), titlesOf(actualPage));
    }

    @Test
    void givenTermsWithSeveralWords_whenCallFindAll_thenRequireAllOfThem() {
        seed();

        final var actualPage =
                gateway.findAll(aQuery("Paul Atreides", null, null, Set.of(), Set.of(), Set.of(), 0, 10));

        assertEquals(List.of("Arrival", "Duna", "Sicario"), titlesOf(actualPage));
    }

    @Test
    void givenEachWordInADifferentField_whenCallFindAll_thenStillFindIt() {
        seed();

        final var actualPage =
                gateway.findAll(aQuery("Duna Arrakis", null, null, Set.of(), Set.of(), Set.of(), 0, 10));

        assertEquals(List.of("Duna"), titlesOf(actualPage));
    }

    private void seed() {
        gateway.save(aVideo("00000001-0000-0000-0000-000000000000", "Duna", Rating.AGE_14, 2026, true, true, references()));
        gateway.save(aVideo(
                "00000002-0000-0000-0000-000000000000",
                "Arrival",
                Rating.AGE_12,
                2016,
                true,
                true,
                VideoReferences.with(Set.of(CategoryID.from("00000008-0000-0000-0000-000000000000")), Set.of(ACAO), Set.of(DENIS))));
        gateway.save(aVideo(
                "00000003-0000-0000-0000-000000000000",
                "Sicario",
                Rating.AGE_16,
                2015,
                true,
                true,
                VideoReferences.with(Set.of(FILMES), Set.of(GenreID.from("00000014-0000-0000-0000-000000000000")), Set.of(DENIS))));
        refresh();
    }

    private void refresh() {
        operations.indexOps(VideoDocument.class).refresh();
    }

    private static VideoReferences references() {
        return VideoReferences.with(Set.of(FILMES), Set.of(ACAO), Set.of(DENIS));
    }

    private static VideoDetails details(final String title, final Rating rating, final int year) {
        return VideoDetails.with(title, "Paul Atreides em Arrakis", Year.of(year), 155.0, rating);
    }

    private static VideoSearchQuery aQuery(
            final String terms,
            final Rating rating,
            final Integer launchedAt,
            final Set<CategoryID> categories,
            final Set<GenreID> genres,
            final Set<CastMemberID> castMembers,
            final int page,
            final int perPage) {
        return new VideoSearchQuery(
                page, perPage, terms, "title", "asc", rating, launchedAt, categories, genres, castMembers);
    }

    private static List<String> titlesOf(final Pagination<Video> page) {
        return page.items().stream().map(v -> v.getDetails().title()).toList();
    }

    private static Video aVideo(
            final String id,
            final String title,
            final Rating rating,
            final int year,
            final boolean active,
            final boolean published,
            final VideoReferences references) {
        return Video.with(
                VideoID.from(id),
                details(title, rating, year),
                new VideoFlags(false, published, active),
                VideoMedias.with("v.mp4", "t.mp4", "b.jpg", "th.jpg", "thh.jpg"),
                references,
                CREATED_AT,
                UPDATED_AT);
    }
}
