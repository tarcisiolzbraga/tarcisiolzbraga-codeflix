package com.tarcisiolzbraga.codeflix.videos.application.castmember.list;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.tarcisiolzbraga.codeflix.videos.application.castmember.CastMemberOutput;
import com.tarcisiolzbraga.codeflix.videos.domain.castmember.CastMember;
import com.tarcisiolzbraga.codeflix.videos.domain.castmember.CastMemberGateway;
import com.tarcisiolzbraga.codeflix.videos.domain.castmember.CastMemberID;
import com.tarcisiolzbraga.codeflix.videos.domain.castmember.CastMemberType;
import com.tarcisiolzbraga.codeflix.videos.domain.pagination.Pagination;
import com.tarcisiolzbraga.codeflix.videos.domain.pagination.SearchQuery;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ListCastMembersUseCaseTest {

    private static final SearchQuery EXPECTED_QUERY = new SearchQuery(0, 10, "den", "name", "asc");
    private static final Instant CREATED_AT = Instant.parse("2026-09-30T12:00:00Z");
    private static final Instant UPDATED_AT = Instant.parse("2026-10-01T08:30:00Z");

    @Mock
    private CastMemberGateway castMemberGateway;

    @InjectMocks
    private DefaultListCastMembersUseCase useCase;

    @Test
    void givenValidQuery_whenCallExecute_thenReturnThePageWithItsMetadata() {
        final var members = List.of(
                aMember("1", "Denis Villeneuve", CastMemberType.DIRECTOR),
                aMember("2", "Timothée Chalamet", CastMemberType.ACTOR));
        when(castMemberGateway.findAll(EXPECTED_QUERY)).thenReturn(new Pagination<>(0, 10, 2L, members));

        final var actualOutput = useCase.execute(EXPECTED_QUERY);

        assertEquals(2L, actualOutput.total());
        assertEquals(
                List.of("Denis Villeneuve", "Timothée Chalamet"),
                actualOutput.items().stream().map(CastMemberOutput::name).toList());
    }

    @Test
    void givenValidQuery_whenCallExecute_thenMapEachMemberToItsOutput() {
        final var member = aMember("1", "Denis Villeneuve", CastMemberType.DIRECTOR);
        when(castMemberGateway.findAll(EXPECTED_QUERY)).thenReturn(new Pagination<>(0, 10, 1L, List.of(member)));

        final var actualOutput = useCase.execute(EXPECTED_QUERY);

        final var actualItem = actualOutput.items().getFirst();
        assertEquals("1", actualItem.id());
        assertEquals(CastMemberType.DIRECTOR, actualItem.type());
        assertTrue(actualItem.active());
        assertEquals(CREATED_AT, actualItem.createdAt());
        assertEquals(UPDATED_AT, actualItem.updatedAt());
    }

    @Test
    void givenQueryWithoutResult_whenCallExecute_thenReturnAnEmptyPage() {
        when(castMemberGateway.findAll(EXPECTED_QUERY)).thenReturn(new Pagination<>(0, 10, 0L, List.of()));

        final var actualOutput = useCase.execute(EXPECTED_QUERY);

        assertEquals(0L, actualOutput.total());
        assertTrue(actualOutput.items().isEmpty());
    }

    @Test
    void givenNullQuery_whenCallExecute_thenThrowNullPointerException() {
        final var actualException = assertThrows(NullPointerException.class, () -> useCase.execute(null));

        assertEquals("'input' should not be null", actualException.getMessage());
    }

    @Test
    void givenFailingGateway_whenCallExecute_thenPropagateTheException() {
        final var expectedException = new IllegalStateException("catálogo indisponível");
        when(castMemberGateway.findAll(EXPECTED_QUERY)).thenThrow(expectedException);

        final var actualException = assertThrows(IllegalStateException.class, () -> useCase.execute(EXPECTED_QUERY));

        assertSame(expectedException, actualException);
    }

    private static CastMember aMember(final String id, final String name, final CastMemberType type) {
        return CastMember.with(CastMemberID.from(id), name, type, true, CREATED_AT, UPDATED_AT);
    }
}
