package com.tarcisiolzbraga.codeflix.videos.application.castmember.get;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tarcisiolzbraga.codeflix.videos.application.castmember.CastMemberOutput;
import com.tarcisiolzbraga.codeflix.videos.domain.castmember.CastMember;
import com.tarcisiolzbraga.codeflix.videos.domain.castmember.CastMemberGateway;
import com.tarcisiolzbraga.codeflix.videos.domain.castmember.CastMemberID;
import com.tarcisiolzbraga.codeflix.videos.domain.castmember.CastMemberType;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GetCastMembersByIdUseCaseTest {

    private static final CastMemberID FIRST_ID = CastMemberID.from("1");
    private static final CastMemberID SECOND_ID = CastMemberID.from("2");
    private static final Instant CREATED_AT = Instant.parse("2026-09-30T12:00:00Z");
    private static final Instant UPDATED_AT = Instant.parse("2026-10-01T08:30:00Z");

    @Mock
    private CastMemberGateway castMemberGateway;

    @InjectMocks
    private DefaultGetCastMembersByIdUseCase useCase;

    @Test
    void givenKnownIds_whenCallExecute_thenReturnOneOutputPerMemberFound() {
        final var ids = Set.of(FIRST_ID, SECOND_ID);
        when(castMemberGateway.findAllById(ids))
                .thenReturn(List.of(
                        aMember(FIRST_ID, "Denis Villeneuve", CastMemberType.DIRECTOR),
                        aMember(SECOND_ID, "Timothée Chalamet", CastMemberType.ACTOR)));

        final var actualOutput = useCase.execute(ids);

        assertEquals(2, actualOutput.size());
        assertEquals(List.of("1", "2"), actualOutput.stream().map(CastMemberOutput::id).toList());
        assertEquals(
                List.of(CastMemberType.DIRECTOR, CastMemberType.ACTOR),
                actualOutput.stream().map(CastMemberOutput::type).toList());
    }

    @Test
    void givenNoId_whenCallExecute_thenReturnEmptyWithoutTouchingTheGateway() {
        final var actualOutput = useCase.execute(Set.of());

        assertTrue(actualOutput.isEmpty());
        verify(castMemberGateway, never()).findAllById(any());
    }

    @Test
    void givenIdsThatTheCatalogDoesNotHave_whenCallExecute_thenReturnOnlyWhatWasFound() {
        final var ids = Set.of(FIRST_ID, SECOND_ID);
        when(castMemberGateway.findAllById(ids))
                .thenReturn(List.of(aMember(FIRST_ID, "Denis Villeneuve", CastMemberType.DIRECTOR)));

        final var actualOutput = useCase.execute(ids);

        assertEquals(1, actualOutput.size());
        assertEquals("1", actualOutput.getFirst().id());
    }

    @Test
    void givenNullInput_whenCallExecute_thenThrowNullPointerException() {
        final var actualException = assertThrows(NullPointerException.class, () -> useCase.execute(null));

        assertEquals("'input' should not be null", actualException.getMessage());
        verify(castMemberGateway, never()).findAllById(any());
    }

    @Test
    void givenFailingGateway_whenCallExecute_thenPropagateTheException() {
        final var ids = Set.of(FIRST_ID);
        final var expectedException = new IllegalStateException("catálogo indisponível");
        when(castMemberGateway.findAllById(ids)).thenThrow(expectedException);

        final var actualException = assertThrows(IllegalStateException.class, () -> useCase.execute(ids));

        assertSame(expectedException, actualException);
    }

    private static CastMember aMember(final CastMemberID id, final String name, final CastMemberType type) {
        return CastMember.with(id, name, type, true, CREATED_AT, UPDATED_AT);
    }
}
