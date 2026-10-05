package com.tarcisiolzbraga.codeflix.videos.infrastructure.graphql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tarcisiolzbraga.codeflix.videos.application.category.CategoryOutput;
import com.tarcisiolzbraga.codeflix.videos.application.category.list.ListCategoriesUseCase;
import com.tarcisiolzbraga.codeflix.videos.application.category.save.SaveCategoryCommand;
import com.tarcisiolzbraga.codeflix.videos.application.category.save.SaveCategoryOutput;
import com.tarcisiolzbraga.codeflix.videos.application.category.save.SaveCategoryUseCase;
import com.tarcisiolzbraga.codeflix.videos.domain.pagination.Pagination;
import com.tarcisiolzbraga.codeflix.videos.domain.pagination.SearchQuery;
import com.tarcisiolzbraga.codeflix.videos.infrastructure.GraphQLControllerTest;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@GraphQLControllerTest(controllers = CategoryGraphQLController.class)
class CategoryGraphQLControllerTest {

    private static final Instant CREATED_AT = Instant.parse("2026-09-30T12:00:00Z");
    private static final Instant UPDATED_AT = Instant.parse("2026-10-01T08:30:00Z");
    private static final String ITEMS_QUERY =
            """
            { categories { items { id name description } } }""";

    @MockitoBean
    private ListCategoriesUseCase listCategoriesUseCase;

    @MockitoBean
    private SaveCategoryUseCase saveCategoryUseCase;

    @Autowired
    private GraphQlTester graphql;

    @Test
    void givenNoArgument_whenCallCategories_thenUseTheDefaultsDeclaredInTheSchema() {
        when(listCategoriesUseCase.execute(any())).thenReturn(emptyPage());

        graphql.document(ITEMS_QUERY).execute();

        final var actualQuery = capturedQuery();
        assertEquals(0, actualQuery.page());
        assertEquals(10, actualQuery.perPage());
        assertEquals("", actualQuery.terms());
        assertEquals("name", actualQuery.sort());
        assertEquals("asc", actualQuery.direction());
    }

    @Test
    void givenEveryArgument_whenCallCategories_thenHandThemToTheUseCase() {
        when(listCategoriesUseCase.execute(any())).thenReturn(emptyPage());
        final var query =
                """
                { categories(search: "fil", page: 2, perPage: 5, sort: "description", direction: "desc")
                  { items { id } } }""";

        graphql.document(query).execute();

        final var actualQuery = capturedQuery();
        assertEquals(2, actualQuery.page());
        assertEquals(5, actualQuery.perPage());
        assertEquals("fil", actualQuery.terms());
        assertEquals("description", actualQuery.sort());
        assertEquals("desc", actualQuery.direction());
    }

    @Test
    void givenAPageOfCategories_whenCallCategories_thenReturnEachItem() {
        when(listCategoriesUseCase.execute(any()))
                .thenReturn(new Pagination<>(0, 10, 2L, List.of(anOutput("1", "Filmes"), anOutput("2", "Séries"))));

        final var actualItems = graphql.document(ITEMS_QUERY)
                .execute()
                .path("categories.items[*].name")
                .entityList(String.class)
                .get();

        assertEquals(List.of("Filmes", "Séries"), actualItems);
    }

    @Test
    void givenAPageOfCategories_whenCallCategories_thenReturnTheNumbersOfThePage() {
        when(listCategoriesUseCase.execute(any()))
                .thenReturn(new Pagination<>(2, 5, 37L, List.of(anOutput("1", "Filmes"))));

        final var actualResult = graphql.document("{ categories { meta { currentPage perPage total } } }").execute();

        actualResult.path("categories.meta.currentPage").entity(Integer.class).isEqualTo(2);
        actualResult.path("categories.meta.perPage").entity(Integer.class).isEqualTo(5);
        actualResult.path("categories.meta.total").entity(Long.class).isEqualTo(37L);
    }

    @Test
    void givenAnEmptyPage_whenCallCategories_thenReturnNoItemAndTotalZero() {
        when(listCategoriesUseCase.execute(any())).thenReturn(emptyPage());

        final var actualResult = graphql.document("{ categories { meta { total } items { id } } }").execute();

        actualResult.path("categories.meta.total").entity(Long.class).isEqualTo(0L);
        assertTrue(actualResult.path("categories.items").entityList(Object.class).get().isEmpty());
    }

    // Fixa o conjunto exato de campos do contrato: pedir um campo que não está no schema tem de ser
    // recusado, e é isso que pega campo a mais entrando sem querer.
    @Test
    void givenAFieldThatIsNotInTheContract_whenCallCategories_thenRefuseTheQuery() {
        final var document = graphql.document("{ categories { items { id active } } }");

        final var actualResponse = document.execute();

        actualResponse.errors().satisfy(errors -> {
            assertEquals(1, errors.size());
            assertTrue(errors.getFirst().getMessage().contains("active"));
        });
    }

    @Test
    void givenAValidInput_whenCallSaveCategory_thenHandEveryValueToTheUseCase() {
        when(saveCategoryUseCase.execute(any())).thenReturn(new SaveCategoryOutput("1"));

        graphql.document(saveDocument("\"Filmes\"", "\"A mais assistida\"", "false")).execute();

        final var captor = ArgumentCaptor.forClass(SaveCategoryCommand.class);
        verify(saveCategoryUseCase).execute(captor.capture());
        final var actualCommand = captor.getValue();
        assertEquals("1", actualCommand.id());
        assertEquals("Filmes", actualCommand.name());
        assertEquals("A mais assistida", actualCommand.description());
        assertEquals(false, actualCommand.active());
        assertEquals(CREATED_AT, actualCommand.createdAt());
        assertEquals(UPDATED_AT, actualCommand.updatedAt());
    }

    @Test
    void givenAValidInput_whenCallSaveCategory_thenReturnWhatWasSaved() {
        when(saveCategoryUseCase.execute(any())).thenReturn(new SaveCategoryOutput("1"));

        final var actualResult =
                graphql.document(saveDocument("\"Filmes\"", "\"A mais assistida\"", "true")).execute();

        actualResult.path("saveCategory.id").entity(String.class).isEqualTo("1");
        actualResult.path("saveCategory.name").entity(String.class).isEqualTo("Filmes");
        actualResult.path("saveCategory.description").entity(String.class).isEqualTo("A mais assistida");
    }

    @Test
    void givenAnInputWithoutActive_whenCallSaveCategory_thenUseTheSchemaDefault() {
        when(saveCategoryUseCase.execute(any())).thenReturn(new SaveCategoryOutput("1"));
        final var document =
                """
                mutation { saveCategory(input: { id: "1", name: "Filmes",
                  createdAt: "2026-09-30T12:00:00Z", updatedAt: "2026-10-01T08:30:00Z" })
                  { id } }""";

        graphql.document(document).execute();

        final var captor = ArgumentCaptor.forClass(SaveCategoryCommand.class);
        verify(saveCategoryUseCase).execute(captor.capture());
        assertEquals(true, captor.getValue().active());
    }

    private static String saveDocument(final String name, final String description, final String active) {
        return """
                mutation { saveCategory(input: { id: "1", name: %s, description: %s, active: %s,
                  createdAt: "2026-09-30T12:00:00Z", updatedAt: "2026-10-01T08:30:00Z" })
                  { id name description } }"""
                .formatted(name, description, active);
    }

    private SearchQuery capturedQuery() {
        final var captor = ArgumentCaptor.forClass(SearchQuery.class);
        verify(listCategoriesUseCase).execute(captor.capture());
        return captor.getValue();
    }

    private static Pagination<CategoryOutput> emptyPage() {
        return new Pagination<>(0, 10, 0L, List.of());
    }

    private static CategoryOutput anOutput(final String id, final String name) {
        return new CategoryOutput(id, name, "A categoria %s".formatted(name), true, CREATED_AT, UPDATED_AT);
    }
}
