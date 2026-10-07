package com.tarcisiolzbraga.codeflix.videos.e2e.genre;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.tarcisiolzbraga.codeflix.videos.e2e.category.CategoryE2EDsl;
import com.tarcisiolzbraga.codeflix.videos.infrastructure.E2ETest;
import com.tarcisiolzbraga.codeflix.videos.infrastructure.KeycloakTestToken;
import com.tarcisiolzbraga.codeflix.videos.infrastructure.genre.GenreClient;
import com.tarcisiolzbraga.codeflix.videos.infrastructure.genre.models.GenreDTO;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.core.io.ClassPathResource;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

// A jornada do gênero, que é a mais completa do projeto: as categorias entram pela mutation de
// exemplo, o gênero entra pelo caminho real do CDC, e o cliente lê tudo por HTTP com as categorias
// resolvidas.
//
// É aqui que a regra "nada inativo vem" fica provada de ponta a ponta: um gênero ativo vinculado a
// uma categoria ativa e a uma inativa sai com a ativa só.
@E2ETest
class GenreE2ETest implements GenreE2EDsl, CategoryE2EDsl {

    private static final Duration VISIBLE = Duration.ofSeconds(40);

    @Value("${local.server.port}")
    private int port;

    @Value("${kafka.consumers.genre.topics}")
    private String topic;

    @Autowired
    private KafkaTemplate<String, String> testKafkaTemplate;

    @Autowired
    private ObjectMapper mapper;

    @MockitoBean
    private GenreClient genreClient;

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
    void givenAGenreWithAnActiveAndAnInactiveCategory_whenAClientReadsIt_thenSeeOnlyTheActiveOne() {
        givenACategory("00000004-0000-0000-0000-000000000000", "Filmes", "A mais assistida");
        givenAnInactiveCategory("00000006-0000-0000-0000-000000000000", "Desativada", "Fora do catálogo");
        givenAGenrePublishedByTheAdmin("00000016-0000-0000-0000-000000000000", "Ação", Set.of("00000004-0000-0000-0000-000000000000", "00000006-0000-0000-0000-000000000000"));

        await().atMost(VISIBLE).untilAsserted(() -> {
            final var actualPage = listGenres();

            assertEquals(1L, actualPage.meta().total());
            final var actualGenre = actualPage.items().getFirst();
            assertEquals("Ação", actualGenre.name());
            assertEquals(List.of("Filmes"), namesOf(actualGenre.categories()));
        });
    }

    @Test
    void givenAGenreFromTheAdmin_whenAClientReadsIt_thenSeeItWithItsCategoriesResolved() {
        givenACategory("00000009-0000-0000-0000-000000000000", "Filmes", "A mais assistida");
        givenACategory("00000010-0000-0000-0000-000000000000", "Séries", "A segunda mais assistida");
        givenAGenrePublishedByTheAdmin("00000016-0000-0000-0000-000000000000", "Ação", Set.of("00000009-0000-0000-0000-000000000000", "00000010-0000-0000-0000-000000000000"));

        await().atMost(VISIBLE).untilAsserted(() -> {
            final var actualItems = listGenres().items();

            // A presença é afirmada antes de ler: getFirst() numa lista vazia lança
            // NoSuchElementException, que não é AssertionError, e o Awaitility não repetiria.
            assertFalse(actualItems.isEmpty());
            assertEquals(List.of("Filmes", "Séries"), namesOf(actualItems.getFirst().categories()));
        });
    }

    @Test
    void givenGenresInDifferentCategories_whenFilterByOne_thenSeeOnlyTheGenresOfThatCategory() {
        givenACategory("00000009-0000-0000-0000-000000000000", "Filmes", "A mais assistida");
        givenACategory("00000010-0000-0000-0000-000000000000", "Séries", "A segunda mais assistida");
        givenAGenrePublishedByTheAdmin("00000016-0000-0000-0000-000000000000", "Ação", Set.of("00000009-0000-0000-0000-000000000000"));
        givenAGenrePublishedByTheAdmin("00000017-0000-0000-0000-000000000000", "Comédia", Set.of("00000010-0000-0000-0000-000000000000"));
        await().atMost(VISIBLE).untilAsserted(() -> assertEquals(2L, listGenres().meta().total()));

        final var actualPage = listGenres(Set.of("00000010-0000-0000-0000-000000000000"));

        assertEquals(1L, actualPage.meta().total());
        assertEquals("Comédia", actualPage.items().getFirst().name());
    }

    @Test
    void givenAGenreWithoutCategories_whenAClientReadsIt_thenSeeAnEmptyList() {
        givenAGenrePublishedByTheAdmin("00000016-0000-0000-0000-000000000000", "Sem categoria", Set.of());

        await().atMost(VISIBLE).untilAsserted(() -> {
            final var actualItems = listGenres().items();

            assertFalse(actualItems.isEmpty());
            assertTrue(actualItems.getFirst().categories().isEmpty());
        });
    }

    @Test
    void givenNothingPublished_whenAClientListsThem_thenSeeAnEmptyPage() {
        final var actualPage = listGenres();

        assertEquals(0L, actualPage.meta().total());
        assertTrue(actualPage.items().isEmpty());
    }

    // Publica no tópico a mensagem que o Debezium publicaria, e prepara a resposta da API do admin,
    // que é de onde as categorias do gênero vêm — a linha do genre não as tem.
    private void givenAGenrePublishedByTheAdmin(final String id, final String name, final Set<String> categories) {
        when(this.genreClient.genreOfId(id))
                .thenReturn(Optional.of(new GenreDTO(
                        id,
                        name,
                        categories,
                        true,
                        Instant.parse("2026-09-30T12:00:00Z"),
                        Instant.parse("2026-10-01T08:30:00Z"))));
        try {
            final var json = new String(
                    new ClassPathResource("cdc/genre-c.json").getInputStream().readAllBytes(),
                    StandardCharsets.UTF_8);
            final var root = (ObjectNode) this.mapper.readTree(json);
            ((ObjectNode) root.get("payload").get("after")).put("id", id);
            this.testKafkaTemplate.send(this.topic, id, this.mapper.writeValueAsString(root)).get();
        } catch (final Exception e) {
            throw new IllegalStateException("não foi possível publicar a mudança de " + id, e);
        }
    }

    private static List<String> namesOf(final List<CategoryView> categories) {
        return categories.stream().map(CategoryView::name).toList();
    }
}
