package com.tarcisiolzbraga.codeflix.videos.e2e.category;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tarcisiolzbraga.codeflix.videos.infrastructure.E2ETest;
import com.tarcisiolzbraga.codeflix.videos.infrastructure.KeycloakTestToken;
import com.tarcisiolzbraga.codeflix.videos.infrastructure.category.models.GqlCategory;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;

// A jornada de quem consome o catálogo: o estado é montado pela própria API e lido de volta por ela,
// sobre HTTP, com Tomcat e Elasticsearch de verdade.
@E2ETest
class CategoryE2ETest implements CategoryE2EDsl {

    // O Elasticsearch é quase em tempo real: o documento gravado só aparece na busca depois do
    // refresh do índice, que por padrão leva até um segundo. Esperar por isso não é contornar o
    // teste — é a mesma espera que o cliente de verdade enfrenta.
    private static final Duration VISIBLE = Duration.ofSeconds(10);

    @Value("${local.server.port}")
    private int port;

    @Autowired
    private KeycloakTestToken token;

    private RestClient client;

    private RestClient adminClient;

    @BeforeEach
    void bindToTheRunningServer() {
        this.client = clientWith(this.token.subscriber());
        this.adminClient = clientWith(this.token.admin());
    }

    // O token vai no cabeçalho padrão do cliente: é como um consumidor de verdade fala com a API,
    // e é o que faz esta jornada atravessar também a autorização.
    private RestClient clientWith(final String bearer) {
        return RestClient.builder()
                .baseUrl("http://localhost:%d/api".formatted(this.port))
                .defaultHeader(HttpHeaders.AUTHORIZATION, bearer)
                .build();
    }

    @Override
    public RestClient client() {
        return this.client;
    }

    @Override
    public RestClient adminClient() {
        return this.adminClient;
    }

    @Test
    void givenCategoriesStoredThroughTheApi_whenListThem_thenSeeThemWithTheNumbersOfThePage() {
        givenACategory("a1", "Filmes", "A mais assistida");
        givenACategory("b2", "Séries", "A segunda mais assistida");

        await().atMost(VISIBLE).untilAsserted(() -> {
            final var actualPage = listCategories();

            assertEquals(0, actualPage.meta().currentPage());
            assertEquals(10, actualPage.meta().perPage());
            assertEquals(2L, actualPage.meta().total());
            assertEquals(List.of("Filmes", "Séries"), namesOf(actualPage.items()));
        });
    }

    @Test
    void givenACategoryStored_whenSearchByATermOnlyInItsDescription_thenFindIt() {
        givenACategory("a1", "Filmes", "A mais assistida");
        givenACategory("b2", "Séries", "A segunda mais assistida");

        await().atMost(VISIBLE).untilAsserted(() -> {
            final var actualPage = listCategories("segunda");

            assertEquals(1L, actualPage.meta().total());
            assertEquals(List.of("Séries"), namesOf(actualPage.items()));
        });
    }

    @Test
    void givenMoreCategoriesThanThePageHolds_whenAskForTheSecondPage_thenSeeTheRestAndTheFullTotal() {
        givenACategory("a1", "Aulas", "Conteúdo didático");
        givenACategory("b2", "Documentários", "Catálogo de documentários");
        givenACategory("c3", "Filmes", "A mais assistida");

        await().atMost(VISIBLE).untilAsserted(() -> {
            final var actualPage = listCategories("", 1, 2);

            assertEquals(1, actualPage.meta().currentPage());
            assertEquals(3L, actualPage.meta().total());
            assertEquals(1, actualPage.items().size());
        });
    }

    @Test
    void givenTheSameIdStoredTwice_whenListThem_thenSeeOnlyTheLastVersion() {
        givenACategory("a1", "Filmes", "A mais assistida");

        givenACategory("a1", "Filmes e séries", "texto novo");

        await().atMost(VISIBLE).untilAsserted(() -> {
            final var actualPage = listCategories();

            assertEquals(1L, actualPage.meta().total());
            assertEquals("Filmes e séries", actualPage.items().getFirst().name());
        });
    }

    @Test
    void givenNothingStored_whenListThem_thenSeeAnEmptyPage() {
        final var actualPage = listCategories();

        assertEquals(0L, actualPage.meta().total());
        assertTrue(actualPage.items().isEmpty());
    }

    private static List<String> namesOf(final List<GqlCategory> items) {
        return items.stream().map(GqlCategory::name).sorted().toList();
    }
}
