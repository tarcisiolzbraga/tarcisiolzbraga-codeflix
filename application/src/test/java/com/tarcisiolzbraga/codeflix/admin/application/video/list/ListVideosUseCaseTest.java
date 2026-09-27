package com.tarcisiolzbraga.codeflix.admin.application.video.list;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tarcisiolzbraga.codeflix.admin.domain.category.CategoryID;
import com.tarcisiolzbraga.codeflix.admin.domain.pagination.Pagination;
import com.tarcisiolzbraga.codeflix.admin.domain.pagination.SearchQuery;
import com.tarcisiolzbraga.codeflix.admin.domain.video.Rating;
import com.tarcisiolzbraga.codeflix.admin.domain.video.Video;
import com.tarcisiolzbraga.codeflix.admin.domain.video.VideoDetails;
import com.tarcisiolzbraga.codeflix.admin.domain.video.VideoGateway;
import com.tarcisiolzbraga.codeflix.admin.domain.video.VideoReferences;
import com.tarcisiolzbraga.codeflix.admin.domain.video.VideoSearchQuery;
import java.time.Year;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ListVideosUseCaseTest {

    private static final SearchQuery PAGE = new SearchQuery(0, 10, null, "title", "asc");

    @Mock
    private VideoGateway videoGateway;

    @InjectMocks
    private DefaultListVideosUseCase useCase;

    @Test
    void givenPersistedVideos_whenCallExecute_thenReturnThemAsOutput() {
        final var video = Video.newVideo(
                VideoDetails.with("Duna", "Arrakis", Year.of(2021), 155.0, Rating.AGE_12), VideoReferences.none());
        when(videoGateway.findAll(any())).thenReturn(new Pagination<>(0, 10, 1, List.of(video)));

        final var actualPage = useCase.execute(VideoSearchQuery.with(PAGE));

        assertEquals(1, actualPage.total());
        final var actualItem = actualPage.items().getFirst();
        assertEquals(video.getId().getValue(), actualItem.id());
        assertEquals("Duna", actualItem.title());
        assertEquals(2021, actualItem.launchedAt());
        assertTrue(actualItem.active());
        assertFalse(actualItem.published());
    }

    @Test
    void givenFiltersByReference_whenCallExecute_thenPassThemToTheGateway() {
        final var category = CategoryID.unique();
        final var query = new VideoSearchQuery(PAGE, Set.of(category), Set.of(), Set.of());
        when(videoGateway.findAll(any())).thenReturn(new Pagination<>(0, 10, 0, List.of()));

        useCase.execute(query);

        verify(videoGateway).findAll(argThat(actual -> actual.categories().equals(Set.of(category))
                && actual.page().equals(PAGE)));
    }

    @Test
    void givenNoVideo_whenCallExecute_thenReturnEmptyPage() {
        when(videoGateway.findAll(any())).thenReturn(new Pagination<>(0, 10, 0, List.of()));

        final var actualPage = useCase.execute(VideoSearchQuery.with(PAGE));

        assertEquals(0, actualPage.total());
        assertTrue(actualPage.items().isEmpty());
    }
}
