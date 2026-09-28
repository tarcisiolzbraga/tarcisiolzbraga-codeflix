package com.tarcisiolzbraga.codeflix.admin.application.video.delete;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

import com.tarcisiolzbraga.codeflix.admin.domain.category.Category;
import com.tarcisiolzbraga.codeflix.admin.domain.category.CategoryGateway;
import com.tarcisiolzbraga.codeflix.admin.domain.video.Video;
import com.tarcisiolzbraga.codeflix.admin.domain.video.VideoFixture;
import com.tarcisiolzbraga.codeflix.admin.domain.video.VideoGateway;
import com.tarcisiolzbraga.codeflix.admin.domain.video.VideoID;
import com.tarcisiolzbraga.codeflix.admin.domain.video.VideoReferences;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.IntegrationTest;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.category.persistence.CategoryRepository;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.video.persistence.VideoRepository;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

@IntegrationTest
class DeleteVideoUseCaseIT {

    @Autowired
    private DeleteVideoUseCase useCase;

    @Autowired
    private CategoryGateway categoryGateway;

    @Autowired
    private VideoRepository videoRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @MockitoSpyBean
    private VideoGateway videoGateway;

    @Test
    void givenVideoWithReferences_whenCallExecute_thenRemoveItAndKeepTheReferences() {
        final var video = givenPersistedVideo();

        this.useCase.execute(video.getId().getValue());

        assertEquals(0, this.videoRepository.count());
        assertEquals(1, this.categoryRepository.count());
    }

    @Test
    void givenUnknownId_whenCallExecute_thenDoNothing() {
        givenPersistedVideo();

        this.useCase.execute(VideoID.unique().getValue());

        assertEquals(1, this.videoRepository.count());
    }

    @Test
    void givenFailingGateway_whenCallExecute_thenPropagateTheException() {
        final var video = givenPersistedVideo();
        final var expectedException = new IllegalStateException("banco indisponível");
        doThrow(expectedException).when(this.videoGateway).deleteById(any());

        final var actualException = assertThrows(
                IllegalStateException.class, () -> this.useCase.execute(video.getId().getValue()));

        assertSame(expectedException, actualException);
        assertEquals(1, this.videoRepository.count());
    }

    private Video givenPersistedVideo() {
        final var category = this.categoryGateway.create(Category.newCategory("Filmes", null, true)).getId();
        return this.videoGateway.create(Video.newVideo(
                VideoFixture.details(),
                VideoReferences.with(Set.of(category), Set.of(), Set.of())));
    }
}
