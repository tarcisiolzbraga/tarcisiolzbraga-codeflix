package com.tarcisiolzbraga.codeflix.videos.application.video.get;

import static com.tarcisiolzbraga.codeflix.videos.application.video.VideoFixture.aVideo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.tarcisiolzbraga.codeflix.videos.domain.video.VideoGateway;
import com.tarcisiolzbraga.codeflix.videos.domain.video.VideoID;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GetVideoUseCaseTest {

    private static final VideoID EXPECTED_ID = VideoID.from("9e2c1b4a-7d3f-4e80-8a51-c2b3d4e5f608");

    @Mock
    private VideoGateway videoGateway;

    @InjectMocks
    private DefaultGetVideoUseCase useCase;

    @Test
    void givenAKnownId_whenCallExecute_thenReturnTheVideo() {
        when(videoGateway.findById(EXPECTED_ID)).thenReturn(Optional.of(aVideo("00000001-0000-0000-0000-000000000000", "Duna", true)));

        final var actualOutput = useCase.execute(EXPECTED_ID);

        assertTrue(actualOutput.isPresent());
        assertEquals("Duna", actualOutput.orElseThrow().details().title());
    }

    // Vazio, não exceção: "não está no catálogo" é resposta normal, e cobre também o vídeo que
    // existe no admin mas o gateway esconde por estar inativo ou não publicado.
    @Test
    void givenAnIdTheCatalogDoesNotServe_whenCallExecute_thenReturnEmpty() {
        when(videoGateway.findById(EXPECTED_ID)).thenReturn(Optional.empty());

        final var actualOutput = useCase.execute(EXPECTED_ID);

        assertTrue(actualOutput.isEmpty());
    }

    @Test
    void givenNullId_whenCallExecute_thenThrowNullPointerException() {
        final var actualException = assertThrows(NullPointerException.class, () -> useCase.execute(null));

        assertEquals("'input' should not be null", actualException.getMessage());
    }

    @Test
    void givenFailingGateway_whenCallExecute_thenPropagateTheException() {
        final var expectedException = new IllegalStateException("catálogo indisponível");
        when(videoGateway.findById(EXPECTED_ID)).thenThrow(expectedException);

        final var actualException = assertThrows(IllegalStateException.class, () -> useCase.execute(EXPECTED_ID));

        assertSame(expectedException, actualException);
    }
}
