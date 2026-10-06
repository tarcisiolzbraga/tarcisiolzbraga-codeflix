package com.tarcisiolzbraga.codeflix.admin.application.castmember.list;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.tarcisiolzbraga.codeflix.admin.domain.castmember.CastMember;
import com.tarcisiolzbraga.codeflix.admin.domain.castmember.CastMemberGateway;
import com.tarcisiolzbraga.codeflix.admin.domain.castmember.CastMemberType;
import com.tarcisiolzbraga.codeflix.admin.domain.pagination.Pagination;
import com.tarcisiolzbraga.codeflix.admin.domain.pagination.SearchQuery;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ListCastMembersUseCaseTest {

    private static final SearchQuery QUERY = new SearchQuery(0, 10, null, "name", "asc");

    @Mock
    private CastMemberGateway castMemberGateway;

    @InjectMocks
    private DefaultListCastMembersUseCase useCase;

    @Test
    void givenPersistedCastMembers_whenCallExecute_thenReturnThemAsOutput() {
        final var member = CastMember.newCastMember("Vin Diesel", CastMemberType.ACTOR, true);
        when(castMemberGateway.findAll(any())).thenReturn(new Pagination<>(0, 10, 1, List.of(member)));

        final var actualPage = useCase.execute(QUERY);

        assertEquals(1, actualPage.total());
        final var actualItem = actualPage.items().getFirst();
        assertEquals(member.getId().getValue(), actualItem.id());
        assertEquals("Vin Diesel", actualItem.name());
        assertEquals("ACTOR", actualItem.type());
        assertTrue(actualItem.isActive());
        assertEquals(member.getCreatedAt(), actualItem.createdAt());
    }

    @Test
    void givenNoCastMember_whenCallExecute_thenReturnEmptyPage() {
        when(castMemberGateway.findAll(any())).thenReturn(new Pagination<>(0, 10, 0, List.of()));

        final var actualPage = useCase.execute(QUERY);

        assertEquals(0, actualPage.total());
        assertTrue(actualPage.items().isEmpty());
    }
}
