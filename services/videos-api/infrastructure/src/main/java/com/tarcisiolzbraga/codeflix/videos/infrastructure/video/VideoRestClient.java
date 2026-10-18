package com.tarcisiolzbraga.codeflix.videos.infrastructure.video;

import com.tarcisiolzbraga.codeflix.videos.infrastructure.video.models.VideoDTO;
import com.tarcisiolzbraga.codeflix.videos.infrastructure.video.models.VideoMediaFile;
import java.util.Objects;
import java.util.Optional;
import org.springframework.http.HttpHeaders;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class VideoRestClient implements VideoClient {

    private static final String BY_ID = "/videos/{id}";

    // O mesmo caminho da rota do admin: o catálogo repassa, não reinventa o endereço.
    private static final String MEDIA = "/videos/{id}/medias/{type}";

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

    // Repassa o arquivo que o admin guarda. **Esta chamada vai autenticada como o próprio catálogo**,
    // pelo interceptor de client credentials do adminRestClient: é o service account dele que tem o
    // papel de vídeos no admin. Quem autoriza o assinante é o controller, antes de chegar aqui.
    @Override
    public Optional<VideoMediaFile> mediaOf(final String id, final String type, final String range) {
        return this.restClient
                .get()
                .uri(MEDIA, id, type)
                // O cabeçalho é repassado como veio: é assim que o player pede trecho, e é assim que
                // o admin e o armazenamento dele esperam receber.
                .headers(headers -> {
                    if (range != null && !range.isBlank()) {
                        headers.set(HttpHeaders.RANGE, range);
                    }
                })
                // `exchange(fn, false)`: a resposta **não** é fechada quando o lambda retorna. É o
                // que permite repasse de verdade — os cabeçalhos são lidos agora, para a rota montar
                // a resposta dela, e o corpo é transferido depois, direto para o cliente. Fechar aqui
                // obrigaria a ler o arquivo inteiro antes de responder, que é o que esta mudança
                // existe para não fazer.
                .exchange((request, response) -> fileFrom(response, id, type), false);
    }

    // Em método próprio, e não no lambda: com os dois ramos (vazio e presente) dentro dele, a
    // inferência do `exchange` não fecha.
    private static Optional<VideoMediaFile> fileFrom(
            final ClientHttpResponse response, final String id, final String type) throws IOException {
        if (response.getStatusCode() == HttpStatus.NOT_FOUND) {
            response.close();
            return Optional.empty();
        }
        // Igual ao videoOfId: 5xx é falha temporária e vira exceção, não "não existe".
        if (!response.getStatusCode().is2xxSuccessful()) {
            final var status = response.getStatusCode();
            response.close();
            throw new IllegalStateException(
                    "admin-codeflix respondeu %s para a mídia %s de %s".formatted(status, type, id));
        }
        final var headers = response.getHeaders();
        return Optional.of(new VideoMediaFile(
                headers.getContentType() == null
                        ? MediaType.APPLICATION_OCTET_STREAM_VALUE
                        : headers.getContentType().toString(),
                filenameOf(headers),
                // Quem abriu fecha: a conexão com o admin vive até aqui, e o `try-with-resources` a
                // solta inclusive quando a escrita falha no meio — cliente que desiste do vídeo é
                // caso comum, não exceção.
                destination -> {
                    try (var open = response) {
                        open.getBody().transferTo(destination);
                    }
                },
                headers.getFirst(HttpHeaders.CONTENT_RANGE)));
    }

    // O nome com que o arquivo foi enviado, que o admin manda no Content-Disposition. Sem ele o
    // download sai com o nome da rota, que não diz nada a quem salvou o arquivo.
    private static String filenameOf(final HttpHeaders headers) {
        final var filename = headers.getContentDisposition().getFilename();
        return filename == null || filename.isBlank() ? "media" : filename;
    }
}
