package com.tarcisiolzbraga.codeflix.admin.e2e.genre;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tarcisiolzbraga.codeflix.admin.domain.category.CategoryID;
import com.tarcisiolzbraga.codeflix.admin.e2e.CategoryE2EDsl;
import com.tarcisiolzbraga.codeflix.admin.e2e.GenreE2EDsl;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.E2ETest;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.KeycloakTestToken;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.api.ApiError;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.genre.models.CreateGenreRequest;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

@E2ETest
class GenreE2ETest implements GenreE2EDsl, CategoryE2EDsl {

    private static final String EXPECTED_NAME = "Ação";

    @Value("${local.server.port}")
    private int port;

    private RestClient client;

    @Autowired
    private KeycloakTestToken token;

    @BeforeEach
    void setUpClient() {
        // Toda chamada leva o token: a API exige role, e o E2E é o único lugar que prova isso contra
        // um emissor de verdade.
        this.client = RestClient.builder()
                .baseUrl("http://localhost:" + this.port)
                .defaultHeader(HttpHeaders.AUTHORIZATION, this.token.bearer())
                .build();
    }

    @Override
    public RestClient client() {
        return this.client;
    }

    @Test
    void givenTwoCategories_whenCreateAGenreWithThem_thenItCanBeRetrievedWithTheCategories() {
        final var movies = givenACategory("Filmes", null);
        final var series = givenACategory("Séries", null);

        final var id = givenAGenre(EXPECTED_NAME, movies, series);

        final var actualGenre = retrieveAGenre(id);
        assertEquals(EXPECTED_NAME, actualGenre.name());
        assertEquals(Stream.of(movies, series).sorted().toList(), actualGenre.categories());
        assertTrue(actualGenre.active());
        assertEquals(actualGenre.createdAt(), actualGenre.updatedAt());
    }

    @Test
    void givenAnUnknownCategoryAndNoName_whenCreate_thenReturnUnprocessableContentWithBothErrors() {
        final var unknown = CategoryID.unique().getValue();
        final var request = new CreateGenreRequest(null, Set.of(unknown), null);

        final var actualException = assertThrows(HttpClientErrorException.class, () -> createAGenre(request));

        assertEquals(HttpStatus.UNPROCESSABLE_CONTENT, actualException.getStatusCode());
        assertEquals(
                List.of("Some categories could not be found: %s".formatted(unknown), "'name' should not be null"),
                actualException.getResponseBodyAs(ApiError.class).errors());
        assertEquals(0, listGenres(0, 10).total());
    }

    // Id que nem UUID é não chega a ser procurado, mas o erro continua se acumulando com os outros:
    // quem mandou a requisição recebe tudo o que está errado nela de uma vez.
    @Test
    void givenAMalformedCategoryAndNoName_whenCreate_thenReturnUnprocessableContentWithBothErrors() {
        final var request = new CreateGenreRequest(null, Set.of("nao-e-um-uuid"), null);

        final var actualException = assertThrows(HttpClientErrorException.class, () -> createAGenre(request));

        assertEquals(HttpStatus.UNPROCESSABLE_CONTENT, actualException.getStatusCode());
        assertEquals(
                List.of("'nao-e-um-uuid' is not a valid CategoryID", "'name' should not be null"),
                actualException.getResponseBodyAs(ApiError.class).errors());
        assertEquals(0, listGenres(0, 10).total());
    }

    @Test
    void givenThreeGenres_whenListTheSecondPage_thenReturnOnlyTheSecondByName() {
        givenAGenre("Terror");
        givenAGenre("Drama");
        givenAGenre(EXPECTED_NAME);

        final var actualPage = listGenres(1, 1);

        assertEquals(3, actualPage.total());
        assertEquals(1, actualPage.currentPage());
        assertEquals("Drama", actualPage.items().getFirst().name());
    }

    @Test
    void givenThreeGenres_whenSearchByATermOfTheName_thenReturnOnlyTheMatch() {
        givenAGenre("Terror");
        givenAGenre("Drama");
        givenAGenre(EXPECTED_NAME);

        final var actualPage = listGenres(0, 10, "ter");

        assertEquals(1, actualPage.total());
        assertEquals("Terror", actualPage.items().getFirst().name());
    }

    @Test
    void givenAGenre_whenUpdateItsNameAndCategories_thenTheApiReturnsTheNewValues() {
        final var id = givenAGenre("Acao", givenACategory("Filmes", null));
        final var series = givenACategory("Séries", null);

        updateAGenre(id, EXPECTED_NAME, series);

        final var actualGenre = retrieveAGenre(id);
        assertEquals(EXPECTED_NAME, actualGenre.name());
        assertEquals(List.of(series), actualGenre.categories());
        assertTrue(actualGenre.updatedAt().isAfter(actualGenre.createdAt()));
    }

    @Test
    void givenAnActiveGenre_whenDeactivateIt_thenItKeepsExistingInactive() {
        final var id = givenAGenre(EXPECTED_NAME);

        final var actualResponse = deactivateAGenre(id);

        assertFalse(actualResponse.active());
        assertFalse(retrieveAGenre(id).active());
        assertEquals(1, listGenres(0, 10).total());
    }

    @Test
    void givenADeactivatedGenre_whenActivateIt_thenItIsActiveAgain() {
        final var id = givenAGenre(EXPECTED_NAME);
        deactivateAGenre(id);

        final var actualResponse = activateAGenre(id);

        assertTrue(actualResponse.active());
        assertTrue(retrieveAGenre(id).active());
    }

    @Test
    void givenAGenreWithACategory_whenDeleteTheGenre_thenItIsGoneAndTheCategoryStays() {
        final var movies = givenACategory("Filmes", null);
        final var id = givenAGenre(EXPECTED_NAME, movies);

        deleteAGenre(id);

        final var actualException = assertThrows(HttpClientErrorException.class, () -> retrieveAGenre(id));
        assertEquals(HttpStatus.NOT_FOUND, actualException.getStatusCode());
        assertEquals(0, listGenres(0, 10).total());
        assertEquals(movies, retrieveACategory(movies).id());
    }
}
