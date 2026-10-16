package com.tarcisiolzbraga.codeflix.videos.infrastructure.category;

import com.tarcisiolzbraga.codeflix.videos.infrastructure.category.models.CategoryDTO;
import java.util.Objects;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class CategoryRestClient implements CategoryClient {

    private static final String BY_ID = "/categories/{id}";

    private final RestClient restClient;

    public CategoryRestClient(final RestClient adminRestClient) {
        this.restClient = Objects.requireNonNull(adminRestClient, "'adminRestClient' should not be null");
    }

    @Override
    public Optional<CategoryDTO> categoryOfId(final String id) {
        return this.restClient
                .get()
                .uri(BY_ID, id)
                .exchange((request, response) -> {
                    if (response.getStatusCode() == HttpStatus.NOT_FOUND) {
                        return Optional.<CategoryDTO>empty();
                    }
                    // Qualquer outro status fora do 2xx vira exceção aqui, e não um Optional vazio:
                    // 500 ou 503 do admin é falha temporária, e tratá-la como "não existe" apagaria
                    // a réplica de uma categoria que está lá.
                    if (!response.getStatusCode().is2xxSuccessful()) {
                        throw new IllegalStateException(
                                "admin-codeflix respondeu %s para %s".formatted(response.getStatusCode(), id));
                    }
                    return Optional.ofNullable(response.bodyTo(CategoryDTO.class));
                });
    }
}
