package com.tarcisiolzbraga.codeflix.videos.infrastructure.kafka;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.tarcisiolzbraga.codeflix.videos.domain.category.CategoryID;
import com.tarcisiolzbraga.codeflix.videos.domain.video.Rating;
import com.tarcisiolzbraga.codeflix.videos.domain.video.VideoGateway;
import com.tarcisiolzbraga.codeflix.videos.domain.video.VideoID;
import com.tarcisiolzbraga.codeflix.videos.infrastructure.IntegrationTest;
import com.tarcisiolzbraga.codeflix.videos.infrastructure.video.VideoClient;
import com.tarcisiolzbraga.codeflix.videos.infrastructure.video.models.ImageMediaDTO;
import com.tarcisiolzbraga.codeflix.videos.infrastructure.video.models.VideoDTO;
import com.tarcisiolzbraga.codeflix.videos.infrastructure.video.models.VideoMediaDTO;
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

// Mensagens de verdade do Debezium, capturadas do tópico adm_videos_mysql.adm_videos.video.
//
// O que as fixtures provam, e estes testes exercitam: a linha do video não tem endereço de mídia nem
// relação alguma, só chaves estrangeiras. Tudo que o catálogo serve vem da resposta do VideoClient.
@IntegrationTest
class VideoListenerIT {

    private static final Duration TIMEOUT = Duration.ofSeconds(30);
    private static final Duration SETTLE = Duration.ofSeconds(3);

    @Value("${kafka.consumers.video.topics}")
    private String topic;

    @Autowired
    private KafkaTemplate<String, String> testKafkaTemplate;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private VideoGateway videoGateway;

    @MockitoBean
    private VideoClient videoClient;

    @Test
    void givenACreateMessage_whenConsumed_thenStoreTheReplicaWithMediaAndRelations() {
        final var id = "11111111-bbbb-4111-8111-111111111111";
        when(videoClient.videoOfId(id)).thenReturn(Optional.of(aDto(id, "Duna", true, true)));

        publish("cdc/video-c.json", id);

        await().atMost(TIMEOUT).untilAsserted(() -> {
            final var stored = videoGateway.findById(VideoID.from(id));
            assertTrue(stored.isPresent());
            assertEquals("Duna", stored.orElseThrow().getDetails().title());
            assertEquals(Rating.AGE_14, stored.orElseThrow().getDetails().rating());
            assertEquals("encoded/duna.mp4", stored.orElseThrow().getMedias().video());
            assertEquals(Set.of(CategoryID.from("c1")), stored.orElseThrow().getReferences().categories());
        });
    }

    // Antes de codificar, o endereço disponível é o cru: o catálogo serve o que o admin tem.
    @Test
    void givenAVideoNotEncodedYet_whenConsumed_thenServeTheRawAddress() {
        final var id = "22222222-bbbb-4222-8222-222222222222";
        final var dto = new VideoDTO(
                id, "Duna", "texto", 2026, 155.0, "14", false, true, true,
                Set.of(), Set.of(), Set.of(),
                new VideoMediaDTO("raw/duna.mp4", null, "PENDING"), null, null, null, null,
                Instant.parse("2026-09-30T12:00:00Z"), Instant.parse("2026-10-01T08:30:00Z"));
        when(videoClient.videoOfId(id)).thenReturn(Optional.of(dto));

        publish("cdc/video-c.json", id);

        await().atMost(TIMEOUT).untilAsserted(() -> {
            final var stored = videoGateway.findById(VideoID.from(id));
            assertTrue(stored.isPresent());
            assertEquals("raw/duna.mp4", stored.orElseThrow().getMedias().video());
        });
    }

    @Test
    void givenAVideoWithoutAnyMedia_whenConsumed_thenStoreItAnyway() {
        final var id = "33333333-bbbb-4333-8333-333333333333";
        final var dto = new VideoDTO(
                id, "Sem midia", "texto", 2026, 155.0, "14", false, true, true,
                Set.of(), Set.of(), Set.of(), null, null, null, null, null,
                Instant.parse("2026-09-30T12:00:00Z"), Instant.parse("2026-10-01T08:30:00Z"));
        when(videoClient.videoOfId(id)).thenReturn(Optional.of(dto));

        publish("cdc/video-c.json", id);

        await().atMost(TIMEOUT).untilAsserted(() -> {
            final var stored = videoGateway.findById(VideoID.from(id));
            assertTrue(stored.isPresent());
            assertNull(stored.orElseThrow().getMedias().video());
        });
    }

    // Replicado, mas não servido: a regra do catálogo é active e published.
    @Test
    void givenAnUnpublishedVideo_whenConsumed_thenStoreItButDoNotServeIt() {
        final var id = "44444444-bbbb-4444-8444-444444444444";
        when(videoClient.videoOfId(id)).thenReturn(Optional.of(aDto(id, "Nao publicado", true, false)));

        publish("cdc/video-c.json", id);

        await().during(SETTLE)
                .atMost(TIMEOUT)
                .untilAsserted(() -> assertFalse(videoGateway.findById(VideoID.from(id)).isPresent()));
    }

    @Test
    void givenADeleteMessage_whenConsumed_thenRemoveTheReplica() {
        final var id = "55555555-bbbb-4555-8555-555555555555";
        when(videoClient.videoOfId(id)).thenReturn(Optional.of(aDto(id, "Vai ser apagado", true, true)));
        publish("cdc/video-c.json", id);
        await().atMost(TIMEOUT)
                .untilAsserted(() -> assertTrue(videoGateway.findById(VideoID.from(id)).isPresent()));

        publish("cdc/video-d.json", id);

        await().atMost(TIMEOUT)
                .untilAsserted(() -> assertTrue(videoGateway.findById(VideoID.from(id)).isEmpty()));
    }

    @Test
    void givenATombstone_whenConsumed_thenIgnoreItAndKeepTheReplica() {
        final var id = "66666666-bbbb-4666-8666-666666666666";
        when(videoClient.videoOfId(id)).thenReturn(Optional.of(aDto(id, "Sobrevive", true, true)));
        publish("cdc/video-c.json", id);
        await().atMost(TIMEOUT)
                .untilAsserted(() -> assertTrue(videoGateway.findById(VideoID.from(id)).isPresent()));

        testKafkaTemplate.send(this.topic, id, null);

        await().during(SETTLE)
                .atMost(TIMEOUT)
                .untilAsserted(() -> assertTrue(videoGateway.findById(VideoID.from(id)).isPresent()));
    }

    @Test
    void givenAMessageForAVideoTheAdminNoLongerHas_whenConsumed_thenStoreNothing() {
        final var id = "77777777-bbbb-4777-8777-777777777777";
        when(videoClient.videoOfId(id)).thenReturn(Optional.empty());

        publish("cdc/video-c.json", id);

        await().during(SETTLE)
                .atMost(TIMEOUT)
                .untilAsserted(() -> assertFalse(videoGateway.findById(VideoID.from(id)).isPresent()));
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

    private static VideoDTO aDto(
            final String id, final String title, final boolean active, final boolean published) {
        return new VideoDTO(
                id,
                title,
                "Paul Atreides em Arrakis",
                2026,
                155.0,
                "14",
                false,
                published,
                active,
                Set.of("c1"),
                Set.of("g1"),
                Set.of("m1"),
                new VideoMediaDTO("raw/duna.mp4", "encoded/duna.mp4", "COMPLETED"),
                new VideoMediaDTO("raw/t.mp4", "encoded/t.mp4", "COMPLETED"),
                new ImageMediaDTO("b.jpg"),
                new ImageMediaDTO("th.jpg"),
                new ImageMediaDTO("thh.jpg"),
                Instant.parse("2026-09-30T12:00:00Z"),
                Instant.parse("2026-10-01T08:30:00Z"));
    }
}
