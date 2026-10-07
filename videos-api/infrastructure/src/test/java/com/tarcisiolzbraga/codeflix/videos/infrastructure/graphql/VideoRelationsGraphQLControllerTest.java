package com.tarcisiolzbraga.codeflix.videos.infrastructure.graphql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tarcisiolzbraga.codeflix.videos.application.castmember.CastMemberOutput;
import com.tarcisiolzbraga.codeflix.videos.application.castmember.get.GetCastMembersByIdUseCase;
import com.tarcisiolzbraga.codeflix.videos.application.category.CategoryOutput;
import com.tarcisiolzbraga.codeflix.videos.application.category.get.GetCategoriesByIdUseCase;
import com.tarcisiolzbraga.codeflix.videos.application.genre.GenreOutput;
import com.tarcisiolzbraga.codeflix.videos.application.genre.get.GetGenresByIdUseCase;
import com.tarcisiolzbraga.codeflix.videos.application.video.VideoOutput;
import com.tarcisiolzbraga.codeflix.videos.application.video.get.GetVideoUseCase;
import com.tarcisiolzbraga.codeflix.videos.application.video.list.ListVideosUseCase;
import com.tarcisiolzbraga.codeflix.videos.domain.castmember.CastMemberID;
import com.tarcisiolzbraga.codeflix.videos.domain.castmember.CastMemberType;
import com.tarcisiolzbraga.codeflix.videos.domain.category.CategoryID;
import com.tarcisiolzbraga.codeflix.videos.domain.genre.GenreID;
import com.tarcisiolzbraga.codeflix.videos.domain.pagination.Pagination;
import com.tarcisiolzbraga.codeflix.videos.domain.video.Rating;
import com.tarcisiolzbraga.codeflix.videos.domain.video.VideoDetails;
import com.tarcisiolzbraga.codeflix.videos.domain.video.VideoFlags;
import com.tarcisiolzbraga.codeflix.videos.domain.video.VideoMedias;
import com.tarcisiolzbraga.codeflix.videos.domain.video.VideoReferences;
import com.tarcisiolzbraga.codeflix.videos.infrastructure.GraphQLControllerTest;
import java.time.Instant;
import java.time.Year;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

// Os dois controllers juntos, porque a consulta vem de um e a resolução dos campos do outro.
@GraphQLControllerTest(
        controllers = {VideoGraphQLController.class, VideoRelationsGraphQLController.class})
class VideoRelationsGraphQLControllerTest {

    private static final Instant CREATED_AT = Instant.parse("2026-09-30T12:00:00Z");
    private static final Instant UPDATED_AT = Instant.parse("2026-10-01T08:30:00Z");

    @MockitoBean
    private ListVideosUseCase listVideosUseCase;

    @MockitoBean
    private GetVideoUseCase getVideoUseCase;

    @MockitoBean
    private GetCategoriesByIdUseCase getCategoriesByIdUseCase;

    @MockitoBean
    private GetGenresByIdUseCase getGenresByIdUseCase;

    @MockitoBean
    private GetCastMembersByIdUseCase getCastMembersByIdUseCase;

    @Autowired
    private GraphQlTester graphql;

    @Test
    void givenAVideoWithTheThreeRelations_whenAskForThem_thenResolveAllOfThem() {
        givenAPageWith(aVideo("00000001-0000-0000-0000-000000000000", Set.of("00000009-0000-0000-0000-000000000000"), Set.of("00000016-0000-0000-0000-000000000000"), Set.of("00000021-0000-0000-0000-000000000000")));
        when(getCategoriesByIdUseCase.execute(any())).thenReturn(List.of(aCategory("00000009-0000-0000-0000-000000000000", "Filmes")));
        when(getGenresByIdUseCase.execute(any())).thenReturn(List.of(aGenre("00000016-0000-0000-0000-000000000000", "Ação")));
        when(getCastMembersByIdUseCase.execute(any())).thenReturn(List.of(aMember("00000021-0000-0000-0000-000000000000", "Denis")));

        final var actualResult = graphql.document(
                        "{ videos { items { categories { name } genres { name } castMembers { name } } } }")
                .execute();

        actualResult.path("videos.items[0].categories[0].name").entity(String.class).isEqualTo("Filmes");
        actualResult.path("videos.items[0].genres[0].name").entity(String.class).isEqualTo("Ação");
        actualResult.path("videos.items[0].castMembers[0].name").entity(String.class).isEqualTo("Denis");
    }

    // Três consultas para a página inteira, não três por vídeo.
    @Test
    void givenSeveralVideos_whenAskForTheRelations_thenCallEachUseCaseOnce() {
        givenAPageWith(
                aVideo("00000001-0000-0000-0000-000000000000", Set.of("00000009-0000-0000-0000-000000000000"), Set.of("00000016-0000-0000-0000-000000000000"), Set.of("00000021-0000-0000-0000-000000000000")),
                aVideo("00000002-0000-0000-0000-000000000000", Set.of("00000010-0000-0000-0000-000000000000"), Set.of("00000017-0000-0000-0000-000000000000"), Set.of("00000022-0000-0000-0000-000000000000")));
        when(getCategoriesByIdUseCase.execute(any())).thenReturn(List.of());
        when(getGenresByIdUseCase.execute(any())).thenReturn(List.of());
        when(getCastMembersByIdUseCase.execute(any())).thenReturn(List.of());

        graphql.document("{ videos { items { categories { id } genres { id } castMembers { id } } } }").execute();

        verify(getCategoriesByIdUseCase, times(1)).execute(any());
        verify(getGenresByIdUseCase, times(1)).execute(any());
        verify(getCastMembersByIdUseCase, times(1)).execute(any());
    }

    @Test
    void givenSeveralVideos_whenAskForTheRelations_thenRequestEveryIdOfThePageAtOnce() {
        givenAPageWith(
                aVideo("00000001-0000-0000-0000-000000000000", Set.of("00000009-0000-0000-0000-000000000000"), Set.of("00000016-0000-0000-0000-000000000000"), Set.of("00000021-0000-0000-0000-000000000000")),
                aVideo("00000002-0000-0000-0000-000000000000", Set.of("00000010-0000-0000-0000-000000000000"), Set.of("00000017-0000-0000-0000-000000000000"), Set.of("00000022-0000-0000-0000-000000000000")));
        when(getCategoriesByIdUseCase.execute(any())).thenReturn(List.of());
        when(getGenresByIdUseCase.execute(any())).thenReturn(List.of());
        when(getCastMembersByIdUseCase.execute(any())).thenReturn(List.of());

        graphql.document("{ videos { items { categories { id } genres { id } castMembers { id } } } }").execute();

        final ArgumentCaptor<Set<CategoryID>> captor = ArgumentCaptor.captor();
        verify(getCategoriesByIdUseCase).execute(captor.capture());
        assertEquals(Set.of(CategoryID.from("00000009-0000-0000-0000-000000000000"), CategoryID.from("00000010-0000-0000-0000-000000000000")), captor.getValue());
    }

    // A regra do catálogo nas três relações de uma vez: o que o caso de uso não devolve — porque
    // está inativo — não aparece no vídeo.
    @Test
    void givenRelationsThatTheUseCasesDoNotReturn_whenAskForThem_thenLeaveThemOutOfTheVideo() {
        givenAPageWith(aVideo("00000001-0000-0000-0000-000000000000", Set.of("00000009-0000-0000-0000-000000000000", "00000006-0000-0000-0000-000000000000"), Set.of("00000016-0000-0000-0000-000000000000", "00000015-0000-0000-0000-000000000000"), Set.of("00000021-0000-0000-0000-000000000000", "00000020-0000-0000-0000-000000000000")));
        when(getCategoriesByIdUseCase.execute(any())).thenReturn(List.of(aCategory("00000009-0000-0000-0000-000000000000", "Filmes")));
        when(getGenresByIdUseCase.execute(any())).thenReturn(List.of(aGenre("00000016-0000-0000-0000-000000000000", "Ação")));
        when(getCastMembersByIdUseCase.execute(any())).thenReturn(List.of(aMember("00000021-0000-0000-0000-000000000000", "Denis")));

        final var actualResult = graphql.document(
                        "{ videos { items { categories { id } genres { id } castMembers { id } } } }")
                .execute();

        assertEquals(
                List.of("00000009-0000-0000-0000-000000000000"),
                actualResult.path("videos.items[0].categories[*].id").entityList(String.class).get());
        assertEquals(
                List.of("00000016-0000-0000-0000-000000000000"), actualResult.path("videos.items[0].genres[*].id").entityList(String.class).get());
        assertEquals(
                List.of("00000021-0000-0000-0000-000000000000"),
                actualResult.path("videos.items[0].castMembers[*].id").entityList(String.class).get());
    }

    @Test
    void givenAVideoWithoutRelations_whenAskForThem_thenReturnThreeEmptyLists() {
        givenAPageWith(aVideo("00000001-0000-0000-0000-000000000000", Set.of(), Set.of(), Set.of()));
        when(getCategoriesByIdUseCase.execute(any())).thenReturn(List.of());
        when(getGenresByIdUseCase.execute(any())).thenReturn(List.of());
        when(getCastMembersByIdUseCase.execute(any())).thenReturn(List.of());

        final var actualResult = graphql.document(
                        "{ videos { items { categories { id } genres { id } castMembers { id } } } }")
                .execute();

        assertTrue(actualResult.path("videos.items[0].categories").entityList(Object.class).get().isEmpty());
        assertTrue(actualResult.path("videos.items[0].genres").entityList(Object.class).get().isEmpty());
        assertTrue(actualResult.path("videos.items[0].castMembers").entityList(Object.class).get().isEmpty());
    }

    // Os ids crus não estão no schema: o cliente não consegue pedi-los.
    @Test
    void givenTheInternalFieldsOfIds_whenAskedFor_thenRefuseTheQuery() {
        final var document = graphql.document("{ videos { items { categoryIds } } }");

        final var actualResponse = document.execute();

        actualResponse.errors().satisfy(errors -> {
            assertEquals(1, errors.size());
            assertTrue(errors.getFirst().getMessage().contains("categoryIds"));
        });
    }

    private void givenAPageWith(final VideoOutput... videos) {
        when(listVideosUseCase.execute(any()))
                .thenReturn(new Pagination<>(0, 10, videos.length, List.of(videos)));
    }

    private static VideoOutput aVideo(
            final String id, final Set<String> categories, final Set<String> genres, final Set<String> members) {
        return new VideoOutput(
                id,
                VideoDetails.with("Duna", "texto", Year.of(2026), 155.0, Rating.AGE_14),
                new VideoFlags(false, true, true),
                VideoMedias.none(),
                VideoReferences.with(
                        categories.stream().map(CategoryID::from).collect(java.util.stream.Collectors.toSet()),
                        genres.stream().map(GenreID::from).collect(java.util.stream.Collectors.toSet()),
                        members.stream().map(CastMemberID::from).collect(java.util.stream.Collectors.toSet())),
                CREATED_AT,
                UPDATED_AT);
    }

    private static CategoryOutput aCategory(final String id, final String name) {
        return new CategoryOutput(id, name, "texto", true, CREATED_AT, UPDATED_AT);
    }

    private static GenreOutput aGenre(final String id, final String name) {
        return new GenreOutput(id, name, true, Set.of(), CREATED_AT, UPDATED_AT);
    }

    private static CastMemberOutput aMember(final String id, final String name) {
        return new CastMemberOutput(id, name, CastMemberType.DIRECTOR, true, CREATED_AT, UPDATED_AT);
    }
}
