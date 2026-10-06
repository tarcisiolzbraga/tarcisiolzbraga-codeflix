package com.tarcisiolzbraga.codeflix.videos.infrastructure.graphql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tarcisiolzbraga.codeflix.videos.application.video.VideoOutput;
import com.tarcisiolzbraga.codeflix.videos.application.video.get.GetVideoUseCase;
import com.tarcisiolzbraga.codeflix.videos.application.video.list.ListVideosUseCase;
import com.tarcisiolzbraga.codeflix.videos.domain.category.CategoryID;
import com.tarcisiolzbraga.codeflix.videos.domain.pagination.Pagination;
import com.tarcisiolzbraga.codeflix.videos.domain.video.Rating;
import com.tarcisiolzbraga.codeflix.videos.domain.video.VideoDetails;
import com.tarcisiolzbraga.codeflix.videos.domain.video.VideoFlags;
import com.tarcisiolzbraga.codeflix.videos.domain.video.VideoID;
import com.tarcisiolzbraga.codeflix.videos.domain.video.VideoMedias;
import com.tarcisiolzbraga.codeflix.videos.domain.video.VideoReferences;
import com.tarcisiolzbraga.codeflix.videos.domain.video.VideoSearchQuery;
import com.tarcisiolzbraga.codeflix.videos.infrastructure.GraphQLControllerTest;
import java.time.Instant;
import java.time.Year;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@GraphQLControllerTest(controllers = VideoGraphQLController.class)
class VideoGraphQLControllerTest {

    private static final Instant CREATED_AT = Instant.parse("2026-09-30T12:00:00Z");
    private static final Instant UPDATED_AT = Instant.parse("2026-10-01T08:30:00Z");

    @MockitoBean
    private ListVideosUseCase listVideosUseCase;

    @MockitoBean
    private GetVideoUseCase getVideoUseCase;

    @Autowired
    private GraphQlTester graphql;

    @Test
    void givenNoArgument_whenCallVideos_thenUseTheDefaultsDeclaredInTheSchema() {
        when(listVideosUseCase.execute(any())).thenReturn(emptyPage());

        graphql.document("{ videos { items { id } } }").execute();

        final var actualQuery = capturedQuery();
        assertEquals(0, actualQuery.page());
        assertEquals(10, actualQuery.perPage());
        assertEquals("title", actualQuery.sort());
        assertEquals("asc", actualQuery.direction());
        assertTrue(actualQuery.categories().isEmpty());
    }

    @Test
    void givenEveryFilter_whenCallVideos_thenHandThemToTheUseCase() {
        when(listVideosUseCase.execute(any())).thenReturn(emptyPage());
        final var document =
                """
                { videos(search: "duna", rating: "14", yearLaunched: 2026,
                         categories: ["c1"], genres: ["g1"], castMembers: ["m1"])
                  { items { id } } }""";

        graphql.document(document).execute();

        final var actualQuery = capturedQuery();
        assertEquals("duna", actualQuery.terms());
        assertEquals(Rating.AGE_14, actualQuery.rating());
        assertEquals(2026, actualQuery.launchedAt());
        assertEquals(Set.of(CategoryID.from("c1")), actualQuery.categories());
    }

    // Rótulo desconhecido é recusado, não ignorado: tratá-lo como "sem filtro" devolveria o acervo
    // inteiro a quem pediu uma classificação específica.
    @Test
    void givenAnUnknownRating_whenCallVideos_thenRefuseTheQueryInsteadOfReturningEverything() {
        final var document = graphql.document("{ videos(rating: \"21\") { items { id } } }");

        final var actualResponse = document.execute();

        actualResponse.errors().satisfy(errors -> {
            assertEquals(1, errors.size());
            assertTrue(errors.getFirst().getMessage().contains("ER, L, 10, 12, 14, 16, 18"));
        });
        verify(listVideosUseCase, never()).execute(any());
    }

    @Test
    void givenAPageOfVideos_whenCallVideos_thenReturnTheItemsAndTheNumbers() {
        when(listVideosUseCase.execute(any()))
                .thenReturn(new Pagination<>(1, 5, 9L, List.of(anOutput("1", "Duna"))));

        final var actualResult =
                graphql.document("{ videos { meta { currentPage total } items { title rating duration } } }").execute();

        actualResult.path("videos.meta.currentPage").entity(Integer.class).isEqualTo(1);
        actualResult.path("videos.meta.total").entity(Long.class).isEqualTo(9L);
        actualResult.path("videos.items[0].title").entity(String.class).isEqualTo("Duna");
        actualResult.path("videos.items[0].rating").entity(String.class).isEqualTo("14");
    }

    @Test
    void givenAKnownId_whenCallVideo_thenReturnIt() {
        when(getVideoUseCase.execute(VideoID.from("1"))).thenReturn(Optional.of(anOutput("1", "Duna")));

        final var actualResult = graphql.document("{ video(id: \"1\") { id title } }").execute();

        actualResult.path("video.title").entity(String.class).isEqualTo("Duna");
    }

    // Nulo quando o catálogo não serve o vídeo, o que inclui inativo e não publicado.
    @Test
    void givenAnIdTheCatalogDoesNotServe_whenCallVideo_thenReturnNull() {
        when(getVideoUseCase.execute(VideoID.from("1"))).thenReturn(Optional.empty());

        final var actualResult = graphql.document("{ video(id: \"1\") { id } }").execute();

        actualResult.path("video").valueIsNull();
    }

    // active e published não estão no schema: tudo que sai daqui já é os dois.
    @Test
    void givenTheFlagsThatAreNotInTheContract_whenAskedFor_thenRefuseTheQuery() {
        final var document = graphql.document("{ videos { items { published } } }");

        final var actualResponse = document.execute();

        actualResponse.errors().satisfy(errors -> {
            assertEquals(1, errors.size());
            assertTrue(errors.getFirst().getMessage().contains("published"));
        });
    }

    private VideoSearchQuery capturedQuery() {
        final var captor = ArgumentCaptor.forClass(VideoSearchQuery.class);
        verify(listVideosUseCase).execute(captor.capture());
        return captor.getValue();
    }

    private static Pagination<VideoOutput> emptyPage() {
        return new Pagination<>(0, 10, 0L, List.of());
    }

    private static VideoOutput anOutput(final String id, final String title) {
        return new VideoOutput(
                id,
                VideoDetails.with(title, "texto", Year.of(2026), 155.0, Rating.AGE_14),
                new VideoFlags(false, true, true),
                VideoMedias.with("v.mp4", "t.mp4", "b.jpg", "th.jpg", "thh.jpg"),
                VideoReferences.none(),
                CREATED_AT,
                UPDATED_AT);
    }
}
