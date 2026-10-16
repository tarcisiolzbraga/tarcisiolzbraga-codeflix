package com.tarcisiolzbraga.codeflix.videos.e2e.video;

import com.tarcisiolzbraga.codeflix.videos.infrastructure.graphql.models.GqlPageMeta;
import java.util.List;
import java.util.Map;
import org.springframework.web.client.RestClient;

// Fala com a API como um cliente qualquer falaria: só HTTP no endpoint de GraphQL.
//
// Os records de leitura são próprios daqui, porque a resposta traz as três relações já resolvidas,
// enquanto o GqlVideo carrega os ids internamente. Ler pelo que o cliente vê é o ponto do teste.
public interface VideoE2EDsl {

    String VIDEO_GRAPHQL_PATH = "/graphql";

    String VIDEO_FIELDS =
            """
            id title rating opened video
            categories { id name }
            genres { id name }
            castMembers { id name type }""";

    RestClient client();

    default VideoPageView listVideos() {
        return listVideos("");
    }

    default VideoPageView listVideos(final String filters) {
        final var document =
                """
                { videos%s { meta { currentPage perPage total } items { %s } } }"""
                        .formatted(filters.isBlank() ? "" : "(" + filters + ")", VIDEO_FIELDS);
        return post(document, ListEnvelope.class).data().videos();
    }

    default VideoView video(final String id) {
        final var document = """
                { video(id: "%s") { %s } }""".formatted(id, VIDEO_FIELDS);
        return post(document, OneEnvelope.class).data().video();
    }

    private <T> T post(final String document, final Class<T> envelope) {
        return client().post()
                .uri(VIDEO_GRAPHQL_PATH)
                .body(Map.of("query", document))
                .retrieve()
                .body(envelope);
    }

    record ListEnvelope(ListData data) {
    }

    record ListData(VideoPageView videos) {
    }

    record OneEnvelope(OneData data) {
    }

    record OneData(VideoView video) {
    }

    record VideoPageView(GqlPageMeta meta, List<VideoView> items) {
    }

    record VideoView(
            String id,
            String title,
            String rating,
            boolean opened,
            String video,
            List<RelationView> categories,
            List<RelationView> genres,
            List<MemberView> castMembers) {
    }

    record RelationView(String id, String name) {
    }

    record MemberView(String id, String name, String type) {
    }
}
