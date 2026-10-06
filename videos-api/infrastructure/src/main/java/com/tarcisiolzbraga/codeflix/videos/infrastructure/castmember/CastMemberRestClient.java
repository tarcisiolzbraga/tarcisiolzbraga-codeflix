package com.tarcisiolzbraga.codeflix.videos.infrastructure.castmember;

import com.tarcisiolzbraga.codeflix.videos.infrastructure.castmember.models.CastMemberDTO;
import java.util.Objects;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class CastMemberRestClient implements CastMemberClient {

    private static final String BY_ID = "/cast-members/{id}";

    private final RestClient restClient;

    public CastMemberRestClient(final RestClient adminRestClient) {
        this.restClient = Objects.requireNonNull(adminRestClient, "'adminRestClient' should not be null");
    }

    @Override
    public Optional<CastMemberDTO> castMemberOfId(final String id) {
        return this.restClient
                .get()
                .uri(BY_ID, id)
                .exchange((request, response) -> {
                    if (response.getStatusCode() == HttpStatus.NOT_FOUND) {
                        return Optional.<CastMemberDTO>empty();
                    }
                    // Status fora do 2xx vira exceção, e não Optional vazio: 5xx é falha temporária,
                    // e tratá-la como "não existe" apagaria a réplica de quem continua lá.
                    if (!response.getStatusCode().is2xxSuccessful()) {
                        throw new IllegalStateException(
                                "admin-codeflix respondeu %s para %s".formatted(response.getStatusCode(), id));
                    }
                    return Optional.ofNullable(response.bodyTo(CastMemberDTO.class));
                });
    }
}
