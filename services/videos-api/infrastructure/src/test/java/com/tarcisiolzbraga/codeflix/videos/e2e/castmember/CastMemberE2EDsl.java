package com.tarcisiolzbraga.codeflix.videos.e2e.castmember;

import com.tarcisiolzbraga.codeflix.videos.infrastructure.castmember.models.GqlCastMemberPage;
import java.util.Map;
import org.springframework.web.client.RestClient;

// Fala com a API como um cliente qualquer falaria: só HTTP no endpoint de GraphQL.
//
// Diferente do DSL da categoria, aqui não há método de escrita: o membro de elenco não tem mutation,
// nem de exemplo. Quem monta o estado da jornada é o Kafka, que é o caminho de verdade do dado.
public interface CastMemberE2EDsl {

    String GRAPHQL_PATH = "/graphql";

    RestClient client();

    default GqlCastMemberPage listCastMembers() {
        return listCastMembers("", 0, 10);
    }

    default GqlCastMemberPage listCastMembers(final String search) {
        return listCastMembers(search, 0, 10);
    }

    default GqlCastMemberPage listCastMembers(final String search, final int page, final int perPage) {
        final var document =
                """
                { castMembers(search: "%s", page: %d, perPage: %d) {
                    meta { currentPage perPage total }
                    items { id name type }
                  } }"""
                        .formatted(search, page, perPage);
        return client().post()
                .uri(GRAPHQL_PATH)
                .body(Map.of("query", document))
                .retrieve()
                .body(ListEnvelope.class)
                .data()
                .castMembers();
    }

    // O corpo de uma resposta GraphQL vem embrulhado em "data".
    record ListEnvelope(ListData data) {
    }

    record ListData(GqlCastMemberPage castMembers) {
    }
}
