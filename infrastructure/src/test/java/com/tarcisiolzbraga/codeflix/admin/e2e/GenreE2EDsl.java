package com.tarcisiolzbraga.codeflix.admin.e2e;

import com.tarcisiolzbraga.codeflix.admin.domain.pagination.Pagination;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.genre.models.CreateGenreRequest;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.genre.models.CreateGenreResponse;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.genre.models.GenreListResponse;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.genre.models.GenreResponse;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.genre.models.UpdateGenreRequest;
import java.util.Optional;
import java.util.Set;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.RestClient;

// Fala com a API como um cliente qualquer falaria: só HTTP, sem atalho pelo gateway ou pelo banco.
public interface GenreE2EDsl {

    String GENRES_PATH = "/genres";

    RestClient client();

    default String givenAGenre(final String name, final String... categories) {
        return createAGenre(new CreateGenreRequest(name, Set.of(categories), null)).id();
    }

    default CreateGenreResponse createAGenre(final CreateGenreRequest request) {
        return client().post()
                .uri(GENRES_PATH)
                .body(request)
                .retrieve()
                .body(CreateGenreResponse.class);
    }

    default GenreResponse retrieveAGenre(final String id) {
        return client().get()
                .uri(GENRES_PATH + "/{id}", id)
                .retrieve()
                .body(GenreResponse.class);
    }

    default Pagination<GenreListResponse> listGenres(final int page, final int perPage) {
        return listGenres(page, perPage, null);
    }

    default Pagination<GenreListResponse> listGenres(final int page, final int perPage, final String search) {
        return client().get()
                .uri(builder -> builder.path(GENRES_PATH)
                        .queryParam("page", page)
                        .queryParam("perPage", perPage)
                        .queryParamIfPresent("search", Optional.ofNullable(search))
                        .build())
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }

    default void updateAGenre(final String id, final String name, final String... categories) {
        client().put()
                .uri(GENRES_PATH + "/{id}", id)
                .body(new UpdateGenreRequest(name, Set.of(categories)))
                .retrieve()
                .toBodilessEntity();
    }

    default GenreResponse activateAGenre(final String id) {
        return changeActivation(id, "activate");
    }

    default GenreResponse deactivateAGenre(final String id) {
        return changeActivation(id, "deactivate");
    }

    default void deleteAGenre(final String id) {
        client().delete().uri(GENRES_PATH + "/{id}", id).retrieve().toBodilessEntity();
    }

    private GenreResponse changeActivation(final String id, final String action) {
        return client().put()
                .uri(GENRES_PATH + "/{id}/{action}", id, action)
                .retrieve()
                .body(GenreResponse.class);
    }
}
