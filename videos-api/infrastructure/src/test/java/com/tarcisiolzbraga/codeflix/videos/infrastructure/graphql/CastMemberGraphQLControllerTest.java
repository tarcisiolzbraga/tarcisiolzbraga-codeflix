package com.tarcisiolzbraga.codeflix.videos.infrastructure.graphql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tarcisiolzbraga.codeflix.videos.application.castmember.CastMemberOutput;
import com.tarcisiolzbraga.codeflix.videos.application.castmember.list.ListCastMembersUseCase;
import com.tarcisiolzbraga.codeflix.videos.domain.castmember.CastMemberType;
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

@GraphQLControllerTest(controllers = CastMemberGraphQLController.class)
class CastMemberGraphQLControllerTest {

    private static final Instant CREATED_AT = Instant.parse("2026-09-30T12:00:00Z");
    private static final Instant UPDATED_AT = Instant.parse("2026-10-01T08:30:00Z");
    private static final String ITEMS_QUERY =
            """
            { castMembers { items { id name type } } }""";

    @MockitoBean
    private ListCastMembersUseCase listCastMembersUseCase;

    @Autowired
    private GraphQlTester graphql;

    @Test
    void givenNoArgument_whenCallCastMembers_thenUseTheDefaultsDeclaredInTheSchema() {
        when(listCastMembersUseCase.execute(any())).thenReturn(emptyPage());

        graphql.document(ITEMS_QUERY).execute();

        final var actualQuery = capturedQuery();
        assertEquals(0, actualQuery.page());
        assertEquals(10, actualQuery.perPage());
        assertEquals("", actualQuery.terms());
        assertEquals("name", actualQuery.sort());
        assertEquals("asc", actualQuery.direction());
    }

    @Test
    void givenEveryArgument_whenCallCastMembers_thenHandThemToTheUseCase() {
        when(listCastMembersUseCase.execute(any())).thenReturn(emptyPage());
        final var query =
                """
                { castMembers(search: "den", page: 2, perPage: 5, sort: "name", direction: "desc")
                  { items { id } } }""";

        graphql.document(query).execute();

        final var actualQuery = capturedQuery();
        assertEquals(2, actualQuery.page());
        assertEquals(5, actualQuery.perPage());
        assertEquals("den", actualQuery.terms());
        assertEquals("desc", actualQuery.direction());
    }

    @Test
    void givenAPageOfMembers_whenCallCastMembers_thenReturnEachItemWithItsType() {
        when(listCastMembersUseCase.execute(any()))
                .thenReturn(new Pagination<>(
                        0,
                        10,
                        2L,
                        List.of(
                                anOutput("00000001-0000-0000-0000-000000000000", "Denis Villeneuve", CastMemberType.DIRECTOR),
                                anOutput("00000002-0000-0000-0000-000000000000", "Timothée Chalamet", CastMemberType.ACTOR))));

        final var actualResult = graphql.document(ITEMS_QUERY).execute();

        assertEquals(
                List.of("Denis Villeneuve", "Timothée Chalamet"),
                actualResult.path("castMembers.items[*].name").entityList(String.class).get());
        assertEquals(
                List.of("DIRECTOR", "ACTOR"),
                actualResult.path("castMembers.items[*].type").entityList(String.class).get());
    }

    @Test
    void givenAPageOfMembers_whenCallCastMembers_thenReturnTheNumbersOfThePage() {
        when(listCastMembersUseCase.execute(any()))
                .thenReturn(new Pagination<>(2, 5, 37L, List.of(anOutput("00000001-0000-0000-0000-000000000000", "Denis", CastMemberType.DIRECTOR))));

        final var actualResult = graphql.document("{ castMembers { meta { currentPage perPage total } } }").execute();

        actualResult.path("castMembers.meta.currentPage").entity(Integer.class).isEqualTo(2);
        actualResult.path("castMembers.meta.perPage").entity(Integer.class).isEqualTo(5);
        actualResult.path("castMembers.meta.total").entity(Long.class).isEqualTo(37L);
    }

    @Test
    void givenAnEmptyPage_whenCallCastMembers_thenReturnNoItemAndTotalZero() {
        when(listCastMembersUseCase.execute(any())).thenReturn(emptyPage());

        final var actualResult = graphql.document("{ castMembers { meta { total } items { id } } }").execute();

        actualResult.path("castMembers.meta.total").entity(Long.class).isEqualTo(0L);
        assertTrue(actualResult.path("castMembers.items").entityList(Object.class).get().isEmpty());
    }

    // Fixa o conjunto exato de campos: pedir um que não está no schema tem de ser recusado.
    @Test
    void givenAFieldThatIsNotInTheContract_whenCallCastMembers_thenRefuseTheQuery() {
        final var document = graphql.document("{ castMembers { items { id active } } }");

        final var actualResponse = document.execute();

        actualResponse.errors().satisfy(errors -> {
            assertEquals(1, errors.size());
            assertTrue(errors.getFirst().getMessage().contains("active"));
        });
    }

    private SearchQuery capturedQuery() {
        final var captor = ArgumentCaptor.forClass(SearchQuery.class);
        verify(listCastMembersUseCase).execute(captor.capture());
        return captor.getValue();
    }

    private static Pagination<CastMemberOutput> emptyPage() {
        return new Pagination<>(0, 10, 0L, List.of());
    }

    private static CastMemberOutput anOutput(final String id, final String name, final CastMemberType type) {
        return new CastMemberOutput(id, name, type, true, CREATED_AT, UPDATED_AT);
    }
}
