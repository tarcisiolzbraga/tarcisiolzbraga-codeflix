package com.tarcisiolzbraga.codeflix.admin.e2e.video;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tarcisiolzbraga.codeflix.admin.e2e.CategoryE2EDsl;
import com.tarcisiolzbraga.codeflix.admin.e2e.VideoE2EDsl;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.E2ETest;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.api.ApiError;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.video.models.CreateVideoRequest;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

@E2ETest
class VideoE2ETest implements VideoE2EDsl, CategoryE2EDsl {

    private static final String DUNA = "Duna";
    private static final String MATRIX = "Matrix";

    @Value("${local.server.port}")
    private int port;

    private RestClient client;

    @BeforeEach
    void setUpClient() {
        this.client = RestClient.create("http://localhost:" + this.port);
    }

    @Override
    public RestClient client() {
        return this.client;
    }

    @Test
    void givenACategory_whenCreateAVideoWithIt_thenItCanBeRetrievedWithTheCategory() {
        final var movies = givenACategory("Filmes", null);

        final var id = givenAVideo(DUNA, movies);

        final var actualVideo = retrieveAVideo(id);
        assertEquals(DUNA, actualVideo.title());
        assertEquals(2021, actualVideo.launchedAt());
        assertEquals("12", actualVideo.rating());
        assertEquals(List.of(movies), actualVideo.categories());
        assertTrue(actualVideo.active());
        assertFalse(actualVideo.published());
        assertFalse(actualVideo.opened());
        assertEquals(actualVideo.createdAt(), actualVideo.updatedAt());
    }

    @Test
    void givenAnUnknownCategoryAndNoTitle_whenCreate_thenReturnUnprocessableContentWithBothErrors() {
        final var unknown = "00000000-0000-0000-0000-000000000000";
        final var request = new CreateVideoRequest(
                null, "Arrakis", 2021, 155.0, "12", Set.of(unknown), Set.of(), Set.of());

        final var actualException = assertThrows(HttpClientErrorException.class, () -> createAVideo(request));

        assertEquals(HttpStatus.UNPROCESSABLE_CONTENT, actualException.getStatusCode());
        final var apiError = actualException.getResponseBodyAs(ApiError.class);
        assertEquals(
                List.of("Some categories could not be found: " + unknown, "'title' should not be null"),
                apiError.errors());
    }

    @Test
    void givenThreeVideos_whenListTheSecondPage_thenReturnOnlyTheSecondByTitle() {
        givenAVideo(DUNA);
        givenAVideo(MATRIX);
        givenAVideo("Alien");

        final var actualPage = listVideos(1, 1);

        assertEquals(3, actualPage.total());
        assertEquals(1, actualPage.currentPage());
        assertEquals(DUNA, actualPage.items().getFirst().title());
    }

    @Test
    void givenTwoVideos_whenSearchByATermOfTheTitle_thenReturnOnlyTheMatch() {
        givenAVideo(DUNA);
        givenAVideo(MATRIX);

        final var actualPage = listVideos(0, 10, "matr");

        assertEquals(1, actualPage.total());
        assertEquals(MATRIX, actualPage.items().getFirst().title());
    }

    @Test
    void givenVideosInDifferentCategories_whenFilterByOne_thenReturnOnlyItsVideos() {
        final var movies = givenACategory("Filmes", null);
        final var series = givenACategory("Séries", null);
        givenAVideo(DUNA, movies);
        givenAVideo(MATRIX, series);

        final var actualPage = listVideosOfCategory(movies);

        assertEquals(1, actualPage.total());
        assertEquals(DUNA, actualPage.items().getFirst().title());
    }

    @Test
    void givenAVideo_whenUpdateItsTitleAndCategories_thenTheApiReturnsTheNewValues() {
        final var movies = givenACategory("Filmes", null);
        final var id = givenAVideo(DUNA);

        updateAVideo(id, "Duna: Parte 2", movies);

        final var actualVideo = retrieveAVideo(id);
        assertEquals("Duna: Parte 2", actualVideo.title());
        assertEquals("14", actualVideo.rating());
        assertEquals(List.of(movies), actualVideo.categories());
    }

    @Test
    void givenAnUnpublishedVideo_whenPublishIt_thenItStaysPublished() {
        final var id = givenAVideo(DUNA);

        final var actualResponse = publishAVideo(id);

        assertTrue(actualResponse.published());
        assertTrue(retrieveAVideo(id).published());
    }

    @Test
    void givenAPublishedVideo_whenUnpublishIt_thenItIsUnpublishedAgain() {
        final var id = givenAVideo(DUNA);
        publishAVideo(id);

        final var actualResponse = unpublishAVideo(id);

        assertFalse(actualResponse.published());
        assertFalse(retrieveAVideo(id).published());
    }

    @Test
    void givenAClosedVideo_whenOpenIt_thenItStaysOpened() {
        final var id = givenAVideo(DUNA);

        final var actualResponse = openAVideo(id);

        assertTrue(actualResponse.opened());
        assertTrue(retrieveAVideo(id).opened());
    }

    @Test
    void givenAnActiveVideo_whenDeactivateIt_thenItKeepsExistingInactive() {
        final var id = givenAVideo(DUNA);

        final var actualResponse = deactivateAVideo(id);

        assertFalse(actualResponse.active());
        assertFalse(retrieveAVideo(id).active());
    }

    @Test
    void givenAVideoWithACategory_whenDeleteTheVideo_thenItIsGoneAndTheCategoryStays() {
        final var movies = givenACategory("Filmes", null);
        final var id = givenAVideo(DUNA, movies);

        deleteAVideo(id);

        final var actualException = assertThrows(HttpClientErrorException.class, () -> retrieveAVideo(id));
        assertEquals(HttpStatus.NOT_FOUND, actualException.getStatusCode());
        assertEquals(0, listVideos(0, 10).total());
        assertEquals(movies, retrieveACategory(movies).id());
    }

    @Test
    void givenACategoryUsedByAVideo_whenDeleteTheCategory_thenReturnConflict() {
        final var movies = givenACategory("Filmes", null);
        givenAVideo(DUNA, movies);

        final var actualException = assertThrows(HttpClientErrorException.class, () -> deleteACategory(movies));

        assertEquals(HttpStatus.CONFLICT, actualException.getStatusCode());
    }
}
