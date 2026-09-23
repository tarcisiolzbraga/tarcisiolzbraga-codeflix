package com.tarcisiolzbraga.codeflix.admin.e2e.category;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tarcisiolzbraga.codeflix.admin.e2e.CategoryE2EDsl;
import com.tarcisiolzbraga.codeflix.admin.e2e.GenreE2EDsl;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.E2ETest;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.api.ApiError;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.category.models.CreateCategoryRequest;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

@E2ETest
class CategoryE2ETest implements CategoryE2EDsl, GenreE2EDsl {

    private static final String EXPECTED_NAME = "Filmes";
    private static final String EXPECTED_DESCRIPTION = "A mais assistida";
    private static final String GENRE_NAME = "Ação";

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
    void givenNoCategory_whenCreateOne_thenItCanBeRetrievedById() {
        final var id = givenACategory(EXPECTED_NAME, EXPECTED_DESCRIPTION);

        final var actualCategory = retrieveACategory(id);

        assertEquals(id, actualCategory.id());
        assertEquals(EXPECTED_NAME, actualCategory.name());
        assertEquals(EXPECTED_DESCRIPTION, actualCategory.description());
        assertTrue(actualCategory.active());
        assertEquals(actualCategory.createdAt(), actualCategory.updatedAt());
    }

    @Test
    void givenThreeCategories_whenListTheSecondPage_thenReturnOnlyTheSecondByName() {
        givenACategory("Séries", null);
        givenACategory("Documentários", null);
        givenACategory(EXPECTED_NAME, null);

        final var actualPage = listCategories(1, 1);

        assertEquals(3, actualPage.total());
        assertEquals(1, actualPage.currentPage());
        assertEquals(1, actualPage.items().size());
        assertEquals(EXPECTED_NAME, actualPage.items().getFirst().name());
    }

    @Test
    void givenThreeCategories_whenSearchByATermOfTheDescription_thenReturnOnlyTheMatch() {
        givenACategory("Séries", "assistidas em casa");
        givenACategory(EXPECTED_NAME, EXPECTED_DESCRIPTION);
        givenACategory("Documentários", "histórias reais");

        final var actualPage = listCategories(0, 10, "casa");

        assertEquals(1, actualPage.total());
        assertEquals("Séries", actualPage.items().getFirst().name());
    }

    @Test
    void givenACategory_whenUpdateIt_thenTheApiReturnsTheNewValues() {
        final var id = givenACategory("Flmes", "descrição antiga");

        updateACategory(id, EXPECTED_NAME, EXPECTED_DESCRIPTION);

        final var actualCategory = retrieveACategory(id);
        assertEquals(EXPECTED_NAME, actualCategory.name());
        assertEquals(EXPECTED_DESCRIPTION, actualCategory.description());
        assertTrue(actualCategory.updatedAt().isAfter(actualCategory.createdAt()));
    }

    @Test
    void givenAnActiveCategory_whenDeactivateIt_thenItKeepsExistingInactive() {
        final var id = givenACategory(EXPECTED_NAME, EXPECTED_DESCRIPTION);

        final var actualResponse = deactivateACategory(id);

        assertFalse(actualResponse.active());
        assertFalse(retrieveACategory(id).active());
        assertEquals(1, listCategories(0, 10).total());
    }

    @Test
    void givenADeactivatedCategory_whenActivateIt_thenItIsActiveAgain() {
        final var id = givenACategory(EXPECTED_NAME, EXPECTED_DESCRIPTION);
        deactivateACategory(id);

        final var actualResponse = activateACategory(id);

        assertTrue(actualResponse.active());
        assertTrue(retrieveACategory(id).active());
    }

    @Test
    void givenACategory_whenDeleteIt_thenRetrievingItReturnsNotFound() {
        final var id = givenACategory(EXPECTED_NAME, EXPECTED_DESCRIPTION);

        deleteACategory(id);

        final var actualException =
                assertThrows(HttpClientErrorException.class, () -> retrieveACategory(id));
        assertEquals(HttpStatus.NOT_FOUND, actualException.getStatusCode());
        assertEquals(0, listCategories(0, 10).total());
    }

    @Test
    void givenABodyWithoutName_whenCreate_thenReturnUnprocessableContentWithTheErrors() {
        final var request = new CreateCategoryRequest(null, EXPECTED_DESCRIPTION, null);

        final var actualException =
                assertThrows(HttpClientErrorException.class, () -> createACategory(request));

        assertEquals(HttpStatus.UNPROCESSABLE_CONTENT, actualException.getStatusCode());
        final var actualError = actualException.getResponseBodyAs(ApiError.class);
        assertEquals("'name' should not be null", actualError.errors().getFirst());
        assertEquals(0, listCategories(0, 10).total());
    }

    @Test
    void givenABodyWithoutTheActiveField_whenCreate_thenTheCategoryIsBornActive() {
        final var request = new CreateCategoryRequest(EXPECTED_NAME, EXPECTED_DESCRIPTION, null);

        final var actualResponse = createACategory(request);

        assertTrue(retrieveACategory(actualResponse.id()).active());
    }

    @Test
    void givenACategoryInAGenre_whenDeleteIt_thenReturnConflictAndKeepIt() {
        final var id = givenACategory(EXPECTED_NAME, EXPECTED_DESCRIPTION);
        givenAGenre(GENRE_NAME, id);

        final var actualException = assertThrows(HttpClientErrorException.class, () -> deleteACategory(id));

        assertEquals(HttpStatus.CONFLICT, actualException.getStatusCode());
        assertEquals(id, retrieveACategory(id).id());
    }

    @Test
    void givenACategoryInAGenre_whenDeactivateIt_thenItStaysInTheGenreInactive() {
        final var id = givenACategory(EXPECTED_NAME, EXPECTED_DESCRIPTION);
        final var genre = givenAGenre(GENRE_NAME, id);

        final var actualResponse = deactivateACategory(id);

        assertFalse(actualResponse.active());
        assertEquals(List.of(id), retrieveAGenre(genre).categories());
    }

    @Test
    void givenACategoryRemovedFromItsGenre_whenDeleteIt_thenItIsGone() {
        final var id = givenACategory(EXPECTED_NAME, EXPECTED_DESCRIPTION);
        final var genre = givenAGenre(GENRE_NAME, id);
        updateAGenre(genre, GENRE_NAME);

        deleteACategory(id);

        assertEquals(0, listCategories(0, 10).total());
    }
}
