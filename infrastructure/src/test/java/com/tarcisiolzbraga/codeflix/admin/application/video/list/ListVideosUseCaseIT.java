package com.tarcisiolzbraga.codeflix.admin.application.video.list;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tarcisiolzbraga.codeflix.admin.domain.category.Category;
import com.tarcisiolzbraga.codeflix.admin.domain.category.CategoryGateway;
import com.tarcisiolzbraga.codeflix.admin.domain.category.CategoryID;
import com.tarcisiolzbraga.codeflix.admin.domain.pagination.SearchQuery;
import com.tarcisiolzbraga.codeflix.admin.domain.video.Video;
import com.tarcisiolzbraga.codeflix.admin.domain.video.VideoFixture;
import com.tarcisiolzbraga.codeflix.admin.domain.video.VideoGateway;
import com.tarcisiolzbraga.codeflix.admin.domain.video.VideoReferences;
import com.tarcisiolzbraga.codeflix.admin.domain.video.VideoSearchQuery;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.IntegrationTest;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@IntegrationTest
class ListVideosUseCaseIT {

    private static final String DUNA = "Duna";
    private static final String MATRIX = "Matrix";
    private static final SearchQuery BY_TITLE = new SearchQuery(0, 10, null, "title", "asc");

    @Autowired
    private ListVideosUseCase useCase;

    @Autowired
    private CategoryGateway categoryGateway;

    @Autowired
    private VideoGateway videoGateway;

    @Test
    void givenPersistedVideos_whenCallExecute_thenReturnThemSortedByTitle() {
        givenPersistedVideo(MATRIX, Set.of());
        givenPersistedVideo(DUNA, Set.of());

        final var actualPage = this.useCase.execute(VideoSearchQuery.with(BY_TITLE));

        assertEquals(2, actualPage.total());
        assertEquals(DUNA, actualPage.items().getFirst().title());
        assertEquals(2021, actualPage.items().getFirst().launchedAt());
        assertEquals(MATRIX, actualPage.items().getLast().title());
    }

    @Test
    void givenTerms_whenCallExecute_thenFilterByTitle() {
        givenPersistedVideo(MATRIX, Set.of());
        givenPersistedVideo(DUNA, Set.of());

        final var actualPage =
                this.useCase.execute(VideoSearchQuery.with(new SearchQuery(0, 10, "dun", "title", "asc")));

        assertEquals(1, actualPage.total());
        assertEquals(DUNA, actualPage.items().getFirst().title());
    }

    @Test
    void givenACategoryFilter_whenCallExecute_thenReturnOnlyTheVideosLinkedToIt() {
        final var movies = existingCategory("Filmes");
        givenPersistedVideo(DUNA, Set.of(movies));
        givenPersistedVideo(MATRIX, Set.of());

        final var actualPage =
                this.useCase.execute(new VideoSearchQuery(BY_TITLE, Set.of(movies), Set.of(), Set.of()));

        assertEquals(1, actualPage.total());
        assertEquals(DUNA, actualPage.items().getFirst().title());
    }

    @Test
    void givenNoVideo_whenCallExecute_thenReturnEmptyPage() {
        final var actualPage = this.useCase.execute(VideoSearchQuery.with(BY_TITLE));

        assertEquals(0, actualPage.total());
        assertTrue(actualPage.items().isEmpty());
    }

    private CategoryID existingCategory(final String name) {
        return this.categoryGateway.create(Category.newCategory(name, null, true)).getId();
    }

    private void givenPersistedVideo(final String title, final Set<CategoryID> categories) {
        this.videoGateway.create(Video.newVideo(
                VideoFixture.details(title),
                VideoReferences.with(categories, Set.of(), Set.of())));
    }
}
