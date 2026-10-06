package com.tarcisiolzbraga.codeflix.videos.application.genre.save;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tarcisiolzbraga.codeflix.videos.domain.category.CategoryID;
import com.tarcisiolzbraga.codeflix.videos.domain.exceptions.DomainException;
import com.tarcisiolzbraga.codeflix.videos.domain.genre.Genre;
import com.tarcisiolzbraga.codeflix.videos.domain.genre.GenreGateway;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SaveGenreUseCaseTest {

    private static final String EXPECTED_ID = "5b8d2e1f-3a4c-4d60-9e71-f2a3b4c5d6e7";
    private static final String EXPECTED_NAME = "Ação";
    private static final Instant CREATED_AT = Instant.parse("2026-09-30T12:00:00Z");
    private static final Instant UPDATED_AT = Instant.parse("2026-10-01T08:30:00Z");

    @Mock
    private GenreGateway genreGateway;

    @InjectMocks
    private DefaultSaveGenreUseCase useCase;

    @Test
    void givenValidCommand_whenCallExecute_thenStoreTheReplicaAndReturnItsId() {
        final var command = aCommand(EXPECTED_NAME, Set.of("c1", "c2"));
        when(genreGateway.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        final var actualOutput = useCase.execute(command);

        assertEquals(EXPECTED_ID, actualOutput.id());
        final var actualGenre = captured();
        assertEquals(EXPECTED_NAME, actualGenre.getName());
        assertEquals(Set.of(CategoryID.from("c1"), CategoryID.from("c2")), actualGenre.getCategories());
        assertEquals(CREATED_AT, actualGenre.getCreatedAt());
    }

    // Gênero sem categoria é válido no admin, e derrubar o consumo por um campo nulo seria pior que
    // replicar sem vínculo: o vínculo volta no evento seguinte.
    @Test
    void givenNullCategories_whenCallExecute_thenTreatThemAsNoneInsteadOfFailing() {
        final var command = aCommand(EXPECTED_NAME, null);
        when(genreGateway.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        useCase.execute(command);

        assertTrue(captured().getCategories().isEmpty());
    }

    @Test
    void givenCategoriesWithANullInside_whenCallExecute_thenDropIt() {
        final var categories = new HashSet<String>();
        categories.add("c1");
        categories.add(null);
        final var command = aCommand(EXPECTED_NAME, categories);
        when(genreGateway.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        useCase.execute(command);

        assertEquals(Set.of(CategoryID.from("c1")), captured().getCategories());
    }

    @Test
    void givenNullId_whenCallExecute_thenThrowDomainExceptionAndNotTouchTheGateway() {
        final var command = new SaveGenreCommand(null, EXPECTED_NAME, true, Set.of(), CREATED_AT, UPDATED_AT);

        final var actualException = assertThrows(DomainException.class, () -> useCase.execute(command));

        assertEquals("'id' should not be null", actualException.getMessage());
        verify(genreGateway, never()).save(any());
    }

    @Test
    void givenBlankName_whenCallExecute_thenThrowDomainExceptionAndNotTouchTheGateway() {
        final var command = aCommand("  ", Set.of());

        final var actualException = assertThrows(DomainException.class, () -> useCase.execute(command));

        assertEquals("'name' should not be empty", actualException.getMessage());
        verify(genreGateway, never()).save(any());
    }

    @Test
    void givenNullCommand_whenCallExecute_thenThrowNullPointerException() {
        final var actualException = assertThrows(NullPointerException.class, () -> useCase.execute(null));

        assertEquals("'input' should not be null", actualException.getMessage());
        verify(genreGateway, never()).save(any());
    }

    @Test
    void givenFailingGateway_whenCallExecute_thenPropagateTheException() {
        final var command = aCommand(EXPECTED_NAME, Set.of());
        final var expectedException = new IllegalStateException("catálogo indisponível");
        when(genreGateway.save(any())).thenThrow(expectedException);

        final var actualException = assertThrows(IllegalStateException.class, () -> useCase.execute(command));

        assertSame(expectedException, actualException);
    }

    private Genre captured() {
        final var captor = ArgumentCaptor.forClass(Genre.class);
        verify(genreGateway).save(captor.capture());
        return captor.getValue();
    }

    private static SaveGenreCommand aCommand(final String name, final Set<String> categories) {
        return new SaveGenreCommand(EXPECTED_ID, name, true, categories, CREATED_AT, UPDATED_AT);
    }
}
