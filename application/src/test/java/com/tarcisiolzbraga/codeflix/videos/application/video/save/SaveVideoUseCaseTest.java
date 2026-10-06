package com.tarcisiolzbraga.codeflix.videos.application.video.save;

import static com.tarcisiolzbraga.codeflix.videos.application.video.VideoFixture.CREATED_AT;
import static com.tarcisiolzbraga.codeflix.videos.application.video.VideoFixture.UPDATED_AT;
import static com.tarcisiolzbraga.codeflix.videos.application.video.VideoFixture.aCommand;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tarcisiolzbraga.codeflix.videos.domain.castmember.CastMemberID;
import com.tarcisiolzbraga.codeflix.videos.domain.category.CategoryID;
import com.tarcisiolzbraga.codeflix.videos.domain.exceptions.DomainException;
import com.tarcisiolzbraga.codeflix.videos.domain.genre.GenreID;
import com.tarcisiolzbraga.codeflix.videos.domain.video.Rating;
import com.tarcisiolzbraga.codeflix.videos.domain.video.Video;
import com.tarcisiolzbraga.codeflix.videos.domain.video.VideoGateway;
import java.time.Year;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SaveVideoUseCaseTest {

    private static final String EXPECTED_ID = "9e2c1b4a-7d3f-4e80-8a51-c2b3d4e5f608";

    @Mock
    private VideoGateway videoGateway;

    @InjectMocks
    private DefaultSaveVideoUseCase useCase;

    @Test
    void givenValidCommand_whenCallExecute_thenStoreTheReplicaAndReturnItsId() {
        when(videoGateway.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        final var actualOutput = useCase.execute(aCommand(EXPECTED_ID, "Duna", "14"));

        assertEquals(EXPECTED_ID, actualOutput.id());
        final var actualVideo = captured();
        assertEquals("Duna", actualVideo.getDetails().title());
        assertEquals(Year.of(2026), actualVideo.getDetails().launchedAt());
        assertEquals(Rating.AGE_14, actualVideo.getDetails().rating());
        assertEquals("v.mp4", actualVideo.getMedias().video());
    }

    @Test
    void givenValidCommand_whenCallExecute_thenConvertTheThreeRelationsToTypedIds() {
        when(videoGateway.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        useCase.execute(aCommand(EXPECTED_ID, "Duna", "14"));

        final var actualReferences = captured().getReferences();
        assertEquals(Set.of(CategoryID.from("c1")), actualReferences.categories());
        assertEquals(Set.of(GenreID.from("g1")), actualReferences.genres());
        assertEquals(Set.of(CastMemberID.from("m1")), actualReferences.castMembers());
    }

    // Classificação desconhecida entra como nulo e o validador a reporta, em vez de a conversão
    // lançar e esconder os outros erros da mesma mensagem.
    @Test
    void givenAnUnknownRating_whenCallExecute_thenThrowDomainExceptionFromTheValidator() {
        final var actualException =
                assertThrows(DomainException.class, () -> useCase.execute(aCommand(EXPECTED_ID, "Duna", "21")));

        assertEquals("'rating' should not be null", actualException.getMessage());
        verify(videoGateway, never()).save(any());
    }

    @Test
    void givenNullReferences_whenCallExecute_thenTreatThemAsNoneInsteadOfFailing() {
        final var command = new SaveVideoCommand(
                EXPECTED_ID,
                new VideoDetailsCommand("Duna", "texto", 2026, 155.0, "14"),
                new VideoFlagsCommand(false, true, true),
                null,
                null,
                CREATED_AT,
                UPDATED_AT);
        when(videoGateway.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        useCase.execute(command);

        final var actualVideo = captured();
        assertTrue(actualVideo.getReferences().categories().isEmpty());
        assertNull(actualVideo.getMedias().video());
    }

    @Test
    void givenNullId_whenCallExecute_thenThrowDomainExceptionAndNotTouchTheGateway() {
        final var actualException =
                assertThrows(DomainException.class, () -> useCase.execute(aCommand(null, "Duna", "14")));

        assertEquals("'id' should not be null", actualException.getMessage());
        verify(videoGateway, never()).save(any());
    }

    @Test
    void givenBlankTitle_whenCallExecute_thenThrowDomainExceptionAndNotTouchTheGateway() {
        final var actualException =
                assertThrows(DomainException.class, () -> useCase.execute(aCommand(EXPECTED_ID, "  ", "14")));

        assertEquals("'title' should not be empty", actualException.getMessage());
        verify(videoGateway, never()).save(any());
    }

    @Test
    void givenNullCommand_whenCallExecute_thenThrowNullPointerException() {
        final var actualException = assertThrows(NullPointerException.class, () -> useCase.execute(null));

        assertEquals("'input' should not be null", actualException.getMessage());
    }

    @Test
    void givenFailingGateway_whenCallExecute_thenPropagateTheException() {
        final var expectedException = new IllegalStateException("catálogo indisponível");
        when(videoGateway.save(any())).thenThrow(expectedException);

        final var actualException = assertThrows(
                IllegalStateException.class, () -> useCase.execute(aCommand(EXPECTED_ID, "Duna", "14")));

        assertSame(expectedException, actualException);
    }

    private Video captured() {
        final var captor = ArgumentCaptor.forClass(Video.class);
        verify(videoGateway).save(captor.capture());
        return captor.getValue();
    }
}
