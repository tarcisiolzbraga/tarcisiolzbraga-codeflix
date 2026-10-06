package com.tarcisiolzbraga.codeflix.videos.e2e.video;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.tarcisiolzbraga.codeflix.videos.e2e.category.CategoryE2EDsl;
import com.tarcisiolzbraga.codeflix.videos.infrastructure.E2ETest;
import com.tarcisiolzbraga.codeflix.videos.infrastructure.KeycloakTestToken;
import com.tarcisiolzbraga.codeflix.videos.infrastructure.castmember.CastMemberClient;
import com.tarcisiolzbraga.codeflix.videos.infrastructure.castmember.models.CastMemberDTO;
import com.tarcisiolzbraga.codeflix.videos.infrastructure.genre.GenreClient;
import com.tarcisiolzbraga.codeflix.videos.infrastructure.genre.models.GenreDTO;
import com.tarcisiolzbraga.codeflix.videos.infrastructure.video.VideoClient;
import com.tarcisiolzbraga.codeflix.videos.infrastructure.video.models.ImageMediaDTO;
import com.tarcisiolzbraga.codeflix.videos.infrastructure.video.models.VideoDTO;
import com.tarcisiolzbraga.codeflix.videos.infrastructure.video.models.VideoMediaDTO;
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

// A jornada mais completa do projeto: quatro agregados entram por três caminhos diferentes — as
// categorias pela mutation de exemplo, o gênero, o membro de elenco e o vídeo pelo CDC — e o cliente
// lê o vídeo por HTTP com as três relações resolvidas.
//
// É aqui que a regra do catálogo fica provada ponta a ponta nas três relações de uma vez: o vídeo
// aponta para uma categoria ativa e uma inativa, um gênero ativo e um inativo, um membro ativo e um
// inativo, e sai com um de cada.
@E2ETest
class VideoE2ETest implements VideoE2EDsl, CategoryE2EDsl {

    private static final Duration VISIBLE = Duration.ofSeconds(60);
    private static final Instant CREATED_AT = Instant.parse("2026-09-30T12:00:00Z");
    private static final Instant UPDATED_AT = Instant.parse("2026-10-01T08:30:00Z");

    @Value("${local.server.port}")
    private int port;

    @Value("${kafka.consumers.video.topics}")
    private String videoTopic;

    @Value("${kafka.consumers.genre.topics}")
    private String genreTopic;

    @Value("${kafka.consumers.cast-member.topics}")
    private String castMemberTopic;

    @Autowired
    private KafkaTemplate<String, String> testKafkaTemplate;

    @Autowired
    private ObjectMapper mapper;

    @MockitoBean
    private VideoClient videoClient;

    @MockitoBean
    private GenreClient genreClient;

    @MockitoBean
    private CastMemberClient castMemberClient;

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
    void givenAVideoWithActiveAndInactiveRelations_whenAClientReadsIt_thenSeeOnlyTheActiveOnes() {
        givenACategory("c-ativa", "Filmes", "A mais assistida");
        givenAnInactiveCategory("c-inativa", "Desativada", "Fora do catálogo");
        givenAGenre("g-ativo", "Ação", true);
        givenAGenre("g-inativo", "Desativado", false);
        givenACastMember("m-ativo", "Denis Villeneuve", true);
        givenACastMember("m-inativo", "Desativado", false);
        givenAVideo("v1", "Duna", true, Set.of("c-ativa", "c-inativa"), Set.of("g-ativo", "g-inativo"), Set.of("m-ativo", "m-inativo"));

        await().atMost(VISIBLE).untilAsserted(() -> {
            final var actualVideo = video("v1");

            assertNotNull(actualVideo, "o vídeo ainda não chegou ao catálogo");
            assertEquals(List.of("Filmes"), actualVideo.categories().stream().map(RelationView::name).toList());
            assertEquals(List.of("Ação"), actualVideo.genres().stream().map(RelationView::name).toList());
            assertEquals(
                    List.of("Denis Villeneuve"),
                    actualVideo.castMembers().stream().map(MemberView::name).toList());
        });
    }

    @Test
    void givenAVideoFromTheAdmin_whenAClientListsThem_thenSeeItWithItsFields() {
        givenAVideo("v1", "Duna", true, Set.of(), Set.of(), Set.of());

        await().atMost(VISIBLE).untilAsserted(() -> {
            final var actualPage = listVideos();

            assertEquals(1L, actualPage.meta().total());
            final var actualVideo = actualPage.items().getFirst();
            assertEquals("Duna", actualVideo.title());
            assertEquals("14", actualVideo.rating());
            assertFalse(actualVideo.opened());
            assertEquals("encoded/duna.mp4", actualVideo.video());
        });
    }

    // A regra que você aprovou: não publicado não é servido, mesmo estando replicado.
    @Test
    void givenAnUnpublishedVideo_whenAClientReadsIt_thenSeeNothing() {
        givenAVideo("v1", "Nao publicado", false, Set.of(), Set.of(), Set.of());

        await().during(Duration.ofSeconds(5)).atMost(VISIBLE).untilAsserted(() -> {
            assertNull(video("v1"));
            assertEquals(0L, listVideos().meta().total());
        });
    }

    @Test
    void givenVideosWithDifferentRatings_whenFilterByOne_thenSeeOnlyThatOne() {
        givenAVideo("v1", "Duna", true, Set.of(), Set.of(), Set.of());
        await().atMost(VISIBLE).untilAsserted(() -> assertEquals(1L, listVideos().meta().total()));

        final var actualPage = listVideos("rating: \"16\"");

        assertEquals(0L, actualPage.meta().total());
    }

    @Test
    void givenAnIdTheCatalogDoesNotHave_whenAClientReadsIt_thenSeeNull() {
        final var actualVideo = video("nao-existe");

        assertNull(actualVideo);
    }

    @Test
    void givenNothingPublished_whenAClientListsThem_thenSeeAnEmptyPage() {
        final var actualPage = listVideos();

        assertEquals(0L, actualPage.meta().total());
        assertTrue(actualPage.items().isEmpty());
    }

    private void givenAGenre(final String id, final String name, final boolean active) {
        when(this.genreClient.genreOfId(id))
                .thenReturn(Optional.of(new GenreDTO(id, name, Set.of(), active, CREATED_AT, UPDATED_AT)));
        publish(this.genreTopic, "cdc/genre-c.json", id);
    }

    private void givenACastMember(final String id, final String name, final boolean active) {
        when(this.castMemberClient.castMemberOfId(id))
                .thenReturn(Optional.of(new CastMemberDTO(id, name, "DIRECTOR", active, CREATED_AT, UPDATED_AT)));
        publish(this.castMemberTopic, "cdc/cast-member-c.json", id);
    }

    private void givenAVideo(
            final String id,
            final String title,
            final boolean published,
            final Set<String> categories,
            final Set<String> genres,
            final Set<String> members) {
        when(this.videoClient.videoOfId(id))
                .thenReturn(Optional.of(new VideoDTO(
                        id, title, "Paul Atreides em Arrakis", 2026, 155.0, "14", false, published, true,
                        categories, genres, members,
                        new VideoMediaDTO("raw/duna.mp4", "encoded/duna.mp4", "COMPLETED"),
                        new VideoMediaDTO("raw/t.mp4", "encoded/t.mp4", "COMPLETED"),
                        new ImageMediaDTO("b.jpg"), new ImageMediaDTO("th.jpg"), new ImageMediaDTO("thh.jpg"),
                        CREATED_AT, UPDATED_AT)));
        publish(this.videoTopic, "cdc/video-c.json", id);
    }

    // Publica no tópico a mensagem que o Debezium publicaria, com o id deste caso.
    private void publish(final String topic, final String resource, final String id) {
        try {
            final var json = new String(
                    new ClassPathResource(resource).getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            final var root = (ObjectNode) this.mapper.readTree(json);
            ((ObjectNode) root.get("payload").get("after")).put("id", id);
            this.testKafkaTemplate.send(topic, id, this.mapper.writeValueAsString(root)).get();
        } catch (final Exception e) {
            throw new IllegalStateException("não foi possível publicar " + resource + " para " + id, e);
        }
    }
}
