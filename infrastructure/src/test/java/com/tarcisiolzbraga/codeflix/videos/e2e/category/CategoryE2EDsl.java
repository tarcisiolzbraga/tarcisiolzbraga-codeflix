package com.tarcisiolzbraga.codeflix.videos.e2e.category;

import com.tarcisiolzbraga.codeflix.videos.infrastructure.category.models.GqlCategory;
import com.tarcisiolzbraga.codeflix.videos.infrastructure.category.models.GqlCategoryPage;
import java.util.Map;
import org.springframework.web.client.RestClient;

// Fala com a API como um cliente qualquer falaria: só HTTP no endpoint de GraphQL, sem atalho pelo
// gateway, pelo caso de uso ou pelo Elasticsearch. É o que faz este teste valer: se o contrato
// quebrar em qualquer camada entre o HTTP e o índice, ele cai.
public interface CategoryE2EDsl {

    String GRAPHQL_PATH = "/graphql";

    RestClient client();

    // A mutation de exemplo é o único caminho de escrita que a API expõe, e é por ela que a jornada
    // monta o estado — o caminho de verdade, o CDC, já é coberto pelo CategoryListenerIT.
    default GqlCategory givenACategory(final String id, final String name, final String description) {
        final var document =
                """
                mutation { saveCategory(input: { id: "%s", name: "%s", description: "%s",
                  createdAt: "2026-09-30T12:00:00Z", updatedAt: "2026-10-01T08:30:00Z" })
                  { id name description } }"""
                        .formatted(id, name, description);
        return post(document, SaveEnvelope.class).data().saveCategory();
    }

    // Para a jornada do gênero provar que categoria inativa não aparece nas relações.
    default GqlCategory givenAnInactiveCategory(final String id, final String name, final String description) {
        final var document =
                """
                mutation { saveCategory(input: { id: "%s", name: "%s", description: "%s", active: false,
                  createdAt: "2026-09-30T12:00:00Z", updatedAt: "2026-10-01T08:30:00Z" })
                  { id name description } }"""
                        .formatted(id, name, description);
        return post(document, SaveEnvelope.class).data().saveCategory();
    }

    default GqlCategoryPage listCategories() {
        return listCategories("", 0, 10);
    }

    default GqlCategoryPage listCategories(final String search) {
        return listCategories(search, 0, 10);
    }

    default GqlCategoryPage listCategories(final String search, final int page, final int perPage) {
        final var document =
                """
                { categories(search: "%s", page: %d, perPage: %d) {
                    meta { currentPage perPage total }
                    items { id name description }
                  } }"""
                        .formatted(search, page, perPage);
        return post(document, ListEnvelope.class).data().categories();
    }

    private <T> T post(final String document, final Class<T> envelope) {
        return client().post()
                .uri(GRAPHQL_PATH)
                .body(Map.of("query", document))
                .retrieve()
                .body(envelope);
    }

    // O corpo de uma resposta GraphQL vem embrulhado em "data"; estes records abrem o embrulho.
    record SaveEnvelope(SaveData data) {
    }

    record SaveData(GqlCategory saveCategory) {
    }

    record ListEnvelope(ListData data) {
    }

    record ListData(GqlCategoryPage categories) {
    }
}
