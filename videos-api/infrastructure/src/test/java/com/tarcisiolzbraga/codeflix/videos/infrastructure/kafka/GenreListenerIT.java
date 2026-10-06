package com.tarcisiolzbraga.codeflix.videos.infrastructure.kafka;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.tarcisiolzbraga.codeflix.videos.domain.category.CategoryID;
import com.tarcisiolzbraga.codeflix.videos.domain.genre.GenreGateway;
import com.tarcisiolzbraga.codeflix.videos.domain.genre.GenreID;
import com.tarcisiolzbraga.codeflix.videos.infrastructure.IntegrationTest;
import com.tarcisiolzbraga.codeflix.videos.infrastructure.genre.GenreClient;
import com.tarcisiolzbraga.codeflix.videos.infrastructure.genre.models.GenreDTO;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

// Mensagens de verdade do Debezium, capturadas do tópico adm_videos_mysql.adm_videos.genre.
//
// Vale notar o que as fixtures provam: a linha do genre não tem categoria alguma. O vínculo só chega
// pela resposta do GenreClient, e é isso que estes testes exercitam.
@IntegrationTest
class GenreListenerIT {

    private static final Duration TIMEOUT = Duration.ofSeconds(30);
    private static final Duration SETTLE = Duration.ofSeconds(3);

    @Value("${kafka.consumers.genre.topics}")
    private String topic;

    @Autowired
    private KafkaTemplate<String, String> testKafkaTemplate;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private GenreGateway genreGateway;

    @MockitoBean
    private GenreClient genreClient;

    @Test
    void givenACreateMessage_whenConsumed_thenFetchTheRecordAndStoreTheReplicaWithItsCategories() {
        final var id = "11111111-aaaa-4111-8111-111111111111";
        when(genreClient.genreOfId(id)).thenReturn(Optional.of(aDto(id, "Ação", Set.of("c1", "c2"))));

        publish("cdc/genre-c.json", id);

        await().atMost(TIMEOUT).untilAsserted(() -> {
            final var stored = genreGateway.findById(GenreID.from(id));
            assertTrue(stored.isPresent());
            assertEquals("Ação", stored.orElseThrow().getName());
            assertEquals(
                    Set.of(CategoryID.from("c1"), CategoryID.from("c2")),
                    stored.orElseThrow().getCategories());
        });
    }

    // O caso que justifica o desenho: o vínculo mudou no admin, a linha do genre foi tocada pelo
    // refreshUpdatedAt, e é a resposta REST que traz a lista nova.
    @Test
    void givenAnUpdateMessage_whenConsumed_thenReplaceTheCategoriesWithWhatTheAdminHasNow() {
        final var id = "22222222-aaaa-4222-8222-222222222222";
        when(genreClient.genreOfId(id)).thenReturn(Optional.of(aDto(id, "Ação", Set.of("c1", "c2"))));
        publish("cdc/genre-c.json", id);
        await().atMost(TIMEOUT).untilAsserted(() -> {
            final var stored = genreGateway.findById(GenreID.from(id));
            assertTrue(stored.isPresent());
            assertEquals(2, stored.orElseThrow().getCategories().size());
        });

        when(genreClient.genreOfId(id)).thenReturn(Optional.of(aDto(id, "Ação", Set.of("c1"))));
        publish("cdc/genre-u.json", id);

        await().atMost(TIMEOUT).untilAsserted(() -> {
            final var stored = genreGateway.findById(GenreID.from(id));
            assertTrue(stored.isPresent());
            assertEquals(Set.of(CategoryID.from("c1")), stored.orElseThrow().getCategories());
        });
    }

    @Test
    void givenADeleteMessage_whenConsumed_thenRemoveTheReplica() {
        final var id = "33333333-aaaa-4333-8333-333333333333";
        when(genreClient.genreOfId(id)).thenReturn(Optional.of(aDto(id, "Vai ser apagado", Set.of("c1"))));
        publish("cdc/genre-c.json", id);
        await().atMost(TIMEOUT)
                .untilAsserted(() -> assertTrue(genreGateway.findById(GenreID.from(id)).isPresent()));

        publish("cdc/genre-d.json", id);

        await().atMost(TIMEOUT)
                .untilAsserted(() -> assertTrue(genreGateway.findById(GenreID.from(id)).isEmpty()));
    }

    @Test
    void givenATombstone_whenConsumed_thenIgnoreItAndKeepTheReplica() {
        final var id = "44444444-aaaa-4444-8444-444444444444";
        when(genreClient.genreOfId(id)).thenReturn(Optional.of(aDto(id, "Sobrevive", Set.of("c1"))));
        publish("cdc/genre-c.json", id);
        await().atMost(TIMEOUT)
                .untilAsserted(() -> assertTrue(genreGateway.findById(GenreID.from(id)).isPresent()));

        testKafkaTemplate.send(this.topic, id, null);

        await().during(SETTLE)
                .atMost(TIMEOUT)
                .untilAsserted(() -> assertTrue(genreGateway.findById(GenreID.from(id)).isPresent()));
    }

    @Test
    void givenAMessageForAGenreTheAdminNoLongerHas_whenConsumed_thenStoreNothing() {
        final var id = "55555555-aaaa-4555-8555-555555555555";
        when(genreClient.genreOfId(id)).thenReturn(Optional.empty());

        publish("cdc/genre-c.json", id);

        await().during(SETTLE)
                .atMost(TIMEOUT)
                .untilAsserted(() -> assertFalse(genreGateway.findById(GenreID.from(id)).isPresent()));
    }

    @Test
    void givenTheAdminReportsAGenreWithoutCategories_whenConsumed_thenStoreItAnyway() {
        final var id = "66666666-aaaa-4666-8666-666666666666";
        when(genreClient.genreOfId(id)).thenReturn(Optional.of(aDto(id, "Sem categoria", Set.of())));

        publish("cdc/genre-c.json", id);

        await().atMost(TIMEOUT).untilAsserted(() -> {
            final var stored = genreGateway.findById(GenreID.from(id));
            assertTrue(stored.isPresent());
            assertTrue(stored.orElseThrow().getCategories().isEmpty());
        });
    }

    private void publish(final String resource, final String id) {
        try {
            final var json = new String(
                    new ClassPathResource(resource).getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            final var root = (ObjectNode) this.mapper.readTree(json);
            final var payload = (ObjectNode) root.get("payload");
            replaceId(payload, "before", id);
            replaceId(payload, "after", id);
            this.testKafkaTemplate.send(this.topic, id, this.mapper.writeValueAsString(root)).get();
        } catch (final Exception e) {
            throw new IllegalStateException("não foi possível publicar " + resource, e);
        }
    }

    private static void replaceId(final ObjectNode payload, final String field, final String id) {
        final var node = payload.get(field);
        if (node != null && node.isObject()) {
            ((ObjectNode) node).put("id", id);
        }
    }

    private static GenreDTO aDto(final String id, final String name, final Set<String> categories) {
        return new GenreDTO(
                id,
                name,
                categories,
                true,
                Instant.parse("2026-09-30T12:00:00Z"),
                Instant.parse("2026-10-01T08:30:00Z"));
    }
}
