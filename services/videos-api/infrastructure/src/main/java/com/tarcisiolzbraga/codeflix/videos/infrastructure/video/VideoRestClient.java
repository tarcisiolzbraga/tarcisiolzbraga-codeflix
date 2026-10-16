package com.tarcisiolzbraga.codeflix.videos.infrastructure.video;

import com.tarcisiolzbraga.codeflix.videos.infrastructure.video.models.VideoDTO;
import java.util.Objects;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class VideoRestClient implements VideoClient {

    private static final String BY_ID = "/videos/{id}";

    private final RestClient restClient;

    public VideoRestClient(final RestClient adminRestClient) {
        this.restClient = Objects.requireNonNull(adminRestClient, "'adminRestClient' should not be null");
    }

    @Override
    public Optional<VideoDTO> videoOfId(final String id) {
        return this.restClient
                .get()
                .uri(BY_ID, id)
                .exchange((request, response) -> {
                    if (response.getStatusCode() == HttpStatus.NOT_FOUND) {
                        return Optional.<VideoDTO>empty();
                    }
                    // Status fora do 2xx vira exceção, não Optional vazio: 5xx é falha temporária, e
                    // tratá-la como "não existe" apagaria a réplica de um vídeo que continua lá.
                    if (!response.getStatusCode().is2xxSuccessful()) {
                        throw new IllegalStateException(
                                "admin-codeflix respondeu %s para %s".formatted(response.getStatusCode(), id));
                    }
                    return Optional.ofNullable(response.bodyTo(VideoDTO.class));
                });
    }
}
