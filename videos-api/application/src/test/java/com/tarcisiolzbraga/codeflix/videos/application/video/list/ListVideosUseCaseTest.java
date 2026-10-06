package com.tarcisiolzbraga.codeflix.videos.application.video.list;

import static com.tarcisiolzbraga.codeflix.videos.application.video.VideoFixture.aVideo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.tarcisiolzbraga.codeflix.videos.domain.category.CategoryID;
import com.tarcisiolzbraga.codeflix.videos.domain.pagination.Pagination;
import com.tarcisiolzbraga.codeflix.videos.domain.video.Rating;
import com.tarcisiolzbraga.codeflix.videos.domain.video.VideoGateway;
import com.tarcisiolzbraga.codeflix.videos.domain.video.VideoSearchQuery;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ListVideosUseCaseTest {

    private static final VideoSearchQuery EXPECTED_QUERY = new VideoSearchQuery(
            0, 10, "duna", "title", "asc", Rating.AGE_14, 2026, Set.of(CategoryID.from("c1")), null, null);

    @Mock
    private VideoGateway videoGateway;

    @InjectMocks
    private DefaultListVideosUseCase useCase;

    @Test
    void givenValidQuery_whenCallExecute_thenReturnThePageWithItsMetadata() {
        when(videoGateway.findAll(EXPECTED_QUERY))
                .thenReturn(new Pagination<>(0, 10, 2L, List.of(aVideo("1", "Duna", true), aVideo("2", "Arrival", true))));

        final var actualOutput = useCase.execute(EXPECTED_QUERY);

        assertEquals(2L, actualOutput.total());
        assertEquals(
                List.of("Duna", "Arrival"),
                actualOutput.items().stream().map(o -> o.details().title()).toList());
    }

    @Test
    void givenValidQuery_whenCallExecute_thenCarryTheValueObjectsOfEachVideo() {
        when(videoGateway.findAll(EXPECTED_QUERY))
                .thenReturn(new Pagination<>(0, 10, 1L, List.of(aVideo("1", "Duna", true))));

        final var actualItem = useCase.execute(EXPECTED_QUERY).items().getFirst();

        assertEquals("1", actualItem.id());
        assertEquals(Rating.AGE_14, actualItem.details().rating());
        assertTrue(actualItem.flags().isVisibleInTheCatalog());
        assertEquals("v.mp4", actualItem.medias().video());
    }

    @Test
    void givenQueryWithoutResult_whenCallExecute_thenReturnAnEmptyPage() {
        when(videoGateway.findAll(EXPECTED_QUERY)).thenReturn(new Pagination<>(0, 10, 0L, List.of()));

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
        when(videoGateway.findAll(EXPECTED_QUERY)).thenThrow(expectedException);

        final var actualException = assertThrows(IllegalStateException.class, () -> useCase.execute(EXPECTED_QUERY));

        assertSame(expectedException, actualException);
    }

}
