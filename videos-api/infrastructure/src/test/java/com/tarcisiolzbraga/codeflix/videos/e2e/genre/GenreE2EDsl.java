package com.tarcisiolzbraga.codeflix.videos.e2e.genre;

import com.tarcisiolzbraga.codeflix.videos.infrastructure.graphql.models.GqlPageMeta;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.web.client.RestClient;

// Fala com a API como um cliente qualquer falaria: só HTTP no endpoint de GraphQL.
//
// Os records de leitura são próprios daqui, e não os Gql* da aplicação: a resposta traz as categorias
// já resolvidas, enquanto o GqlGenre carrega os ids internamente. Ler pelo que o cliente vê é o
// ponto do teste.
public interface GenreE2EDsl {

    String GENRE_GRAPHQL_PATH = "/graphql";

    RestClient client();

    default GenrePageView listGenres() {
        return listGenres(Set.of());
    }

    default GenrePageView listGenres(final Set<String> categories) {
        final var filter = categories.isEmpty()
                ? ""
                : "(categories: [%s])".formatted(categories.stream().map("\"%s\""::formatted).reduce((a, b) -> a + ", " + b).orElse(""));
        final var document =
                """
                { genres%s {
                    meta { currentPage perPage total }
                    items { id name categories { id name } }
                  } }"""
                        .formatted(filter);
        return client().post()
                .uri(GENRE_GRAPHQL_PATH)
                .body(Map.of("query", document))
                .retrieve()
                .body(ListEnvelope.class)
                .data()
                .genres();
    }

    record ListEnvelope(ListData data) {
    }

    record ListData(GenrePageView genres) {
    }

    record GenrePageView(GqlPageMeta meta, List<GenreView> items) {
    }

    record GenreView(String id, String name, List<CategoryView> categories) {
    }

    record CategoryView(String id, String name) {
    }
}
