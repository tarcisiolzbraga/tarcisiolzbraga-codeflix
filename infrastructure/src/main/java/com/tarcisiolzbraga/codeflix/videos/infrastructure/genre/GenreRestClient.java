package com.tarcisiolzbraga.codeflix.videos.infrastructure.genre;

import com.tarcisiolzbraga.codeflix.videos.infrastructure.genre.models.GenreDTO;
import java.util.Objects;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class GenreRestClient implements GenreClient {

    private static final String BY_ID = "/genres/{id}";

    private final RestClient restClient;

    public GenreRestClient(final RestClient adminRestClient) {
        this.restClient = Objects.requireNonNull(adminRestClient, "'adminRestClient' should not be null");
    }

    @Override
    public Optional<GenreDTO> genreOfId(final String id) {
        return this.restClient
                .get()
                .uri(BY_ID, id)
                .exchange((request, response) -> {
                    if (response.getStatusCode() == HttpStatus.NOT_FOUND) {
                        return Optional.<GenreDTO>empty();
                    }
                    // Status fora do 2xx vira exceção, não Optional vazio: 5xx é falha temporária, e
                    // tratá-la como "não existe" apagaria a réplica de um gênero que continua lá.
                    if (!response.getStatusCode().is2xxSuccessful()) {
                        throw new IllegalStateException(
                                "admin-codeflix respondeu %s para %s".formatted(response.getStatusCode(), id));
                    }
                    return Optional.ofNullable(response.bodyTo(GenreDTO.class));
                });
    }
}
