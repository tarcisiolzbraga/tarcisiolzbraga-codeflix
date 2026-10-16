package com.tarcisiolzbraga.codeflix.videos.infrastructure.graphql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tarcisiolzbraga.codeflix.videos.application.category.CategoryOutput;
import com.tarcisiolzbraga.codeflix.videos.application.category.get.GetCategoriesByIdUseCase;
import com.tarcisiolzbraga.codeflix.videos.application.genre.GenreOutput;
import com.tarcisiolzbraga.codeflix.videos.application.genre.list.ListGenresUseCase;
import com.tarcisiolzbraga.codeflix.videos.domain.category.CategoryID;
import com.tarcisiolzbraga.codeflix.videos.domain.genre.GenreSearchQuery;
import com.tarcisiolzbraga.codeflix.videos.domain.pagination.Pagination;
import com.tarcisiolzbraga.codeflix.videos.infrastructure.GraphQLControllerTest;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@GraphQLControllerTest(controllers = GenreGraphQLController.class)
class GenreGraphQLControllerTest {

    private static final Instant CREATED_AT = Instant.parse("2026-09-30T12:00:00Z");
    private static final Instant UPDATED_AT = Instant.parse("2026-10-01T08:30:00Z");

    @MockitoBean
    private ListGenresUseCase listGenresUseCase;

    @MockitoBean
    private GetCategoriesByIdUseCase getCategoriesByIdUseCase;

    @Autowired
    private GraphQlTester graphql;

    @Test
    void givenNoArgument_whenCallGenres_thenUseTheDefaultsDeclaredInTheSchema() {
        when(listGenresUseCase.execute(any())).thenReturn(emptyPage());

        graphql.document("{ genres { items { id } } }").execute();

        final var actualQuery = capturedQuery();
        assertEquals(0, actualQuery.page());
        assertEquals(10, actualQuery.perPage());
        assertEquals("", actualQuery.terms());
        assertEquals("name", actualQuery.sort());
        assertEquals("asc", actualQuery.direction());
        assertTrue(actualQuery.categories().isEmpty());
    }

    @Test
    void givenCategoriesToFilterBy_whenCallGenres_thenHandThemToTheUseCaseAsTypedIds() {
        when(listGenresUseCase.execute(any())).thenReturn(emptyPage());

        graphql.document("{ genres(categories: [\"00000009-0000-0000-0000-000000000000\", \"00000010-0000-0000-0000-000000000000\"]) { items { id } } }").execute();

        assertEquals(
                Set.of(CategoryID.from("00000009-0000-0000-0000-000000000000"), CategoryID.from("00000010-0000-0000-0000-000000000000")), capturedQuery().categories());
    }

    @Test
    void givenAPageOfGenres_whenCallGenres_thenReturnTheItemsAndTheNumbers() {
        when(listGenresUseCase.execute(any()))
                .thenReturn(new Pagination<>(1, 5, 9L, List.of(aGenre("00000001-0000-0000-0000-000000000000", "Ação", Set.of()))));

        final var actualResult = graphql.document("{ genres { meta { currentPage perPage total } items { name } } }")
                .execute();

        actualResult.path("genres.meta.currentPage").entity(Integer.class).isEqualTo(1);
        actualResult.path("genres.meta.total").entity(Long.class).isEqualTo(9L);
        assertEquals(
                List.of("Ação"), actualResult.path("genres.items[*].name").entityList(String.class).get());
    }

    @Test
    void givenGenresWithCategories_whenAskForThem_thenResolveTheCategoriesInOneCall() {
        when(listGenresUseCase.execute(any()))
                .thenReturn(new Pagination<>(
                        0,
                        10,
                        2L,
                        List.of(aGenre("00000001-0000-0000-0000-000000000000", "Ação", Set.of("00000009-0000-0000-0000-000000000000")), aGenre("00000002-0000-0000-0000-000000000000", "Comédia", Set.of("00000009-0000-0000-0000-000000000000", "00000010-0000-0000-0000-000000000000")))));
        when(getCategoriesByIdUseCase.execute(any()))
                .thenReturn(List.of(aCategory("00000009-0000-0000-0000-000000000000", "Filmes"), aCategory("00000010-0000-0000-0000-000000000000", "Séries")));

        final var actualResult = graphql.document("{ genres { items { name categories { id name } } } }").execute();

        assertEquals(
                List.of("Filmes", "Filmes", "Séries"),
                actualResult.path("genres.items[*].categories[*].name").entityList(String.class).get());
        // Uma chamada só para a página inteira, não uma por gênero.
        verify(getCategoriesByIdUseCase, times(1)).execute(any());
    }

    @Test
    void givenGenresWithCategories_whenAskForThem_thenRequestEveryIdOfThePageAtOnce() {
        when(listGenresUseCase.execute(any()))
                .thenReturn(new Pagination<>(
                        0,
                        10,
                        2L,
                        List.of(aGenre("00000001-0000-0000-0000-000000000000", "Ação", Set.of("00000009-0000-0000-0000-000000000000")), aGenre("00000002-0000-0000-0000-000000000000", "Comédia", Set.of("00000010-0000-0000-0000-000000000000")))));
        when(getCategoriesByIdUseCase.execute(any())).thenReturn(List.of());

        graphql.document("{ genres { items { categories { id } } } }").execute();

        // captor(), e não forClass(Set.class): o genérico é preservado, sem aviso de unchecked.
        final ArgumentCaptor<Set<CategoryID>> captor = ArgumentCaptor.captor();
        verify(getCategoriesByIdUseCase).execute(captor.capture());
        assertEquals(Set.of(CategoryID.from("00000009-0000-0000-0000-000000000000"), CategoryID.from("00000010-0000-0000-0000-000000000000")), captor.getValue());
    }

    // A regra do catálogo cumprida sem código próprio: a categoria inativa não volta do caso de uso,
    // então o gênero sai sem ela.
    @Test
    void givenACategoryThatTheUseCaseDoesNotReturn_whenAskForTheCategories_thenLeaveItOutOfTheGenre() {
        when(listGenresUseCase.execute(any()))
                .thenReturn(new Pagination<>(0, 10, 1L, List.of(aGenre("00000001-0000-0000-0000-000000000000", "Ação", Set.of("00000009-0000-0000-0000-000000000000", "00000006-0000-0000-0000-000000000000")))));
        when(getCategoriesByIdUseCase.execute(any())).thenReturn(List.of(aCategory("00000009-0000-0000-0000-000000000000", "Filmes")));

        final var actualResult = graphql.document("{ genres { items { categories { id } } } }").execute();

        assertEquals(
                List.of("00000009-0000-0000-0000-000000000000"),
                actualResult.path("genres.items[*].categories[*].id").entityList(String.class).get());
    }

    @Test
    void givenAGenreWithoutCategories_whenAskForThem_thenReturnAnEmptyList() {
        when(listGenresUseCase.execute(any()))
                .thenReturn(new Pagination<>(0, 10, 1L, List.of(aGenre("00000001-0000-0000-0000-000000000000", "Ação", Set.of()))));
        when(getCategoriesByIdUseCase.execute(any())).thenReturn(List.of());

        final var actualResult = graphql.document("{ genres { items { categories { id } } } }").execute();

        assertTrue(actualResult.path("genres.items[0].categories").entityList(Object.class).get().isEmpty());
    }

    // Os ids crus não estão no schema de propósito: o cliente não consegue pedi-los.
    @Test
    void givenTheInternalFieldOfIds_whenAskedFor_thenRefuseTheQuery() {
        final var document = graphql.document("{ genres { items { categoryIds } } }");

        final var actualResponse = document.execute();

        actualResponse.errors().satisfy(errors -> {
            assertEquals(1, errors.size());
            assertTrue(errors.getFirst().getMessage().contains("categoryIds"));
        });
    }

    private GenreSearchQuery capturedQuery() {
        final var captor = ArgumentCaptor.forClass(GenreSearchQuery.class);
        verify(listGenresUseCase).execute(captor.capture());
        return captor.getValue();
    }

    private static Pagination<GenreOutput> emptyPage() {
        return new Pagination<>(0, 10, 0L, List.of());
    }

    private static GenreOutput aGenre(final String id, final String name, final Set<String> categories) {
        return new GenreOutput(
                id,
                name,
                true,
                categories.stream().map(CategoryID::from).collect(java.util.stream.Collectors.toUnmodifiableSet()),
                CREATED_AT,
                UPDATED_AT);
    }

    private static CategoryOutput aCategory(final String id, final String name) {
        return new CategoryOutput(id, name, "A categoria " + name, true, CREATED_AT, UPDATED_AT);
    }
}
