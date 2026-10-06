package com.tarcisiolzbraga.codeflix.videos.infrastructure.kafka;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.tarcisiolzbraga.codeflix.videos.domain.castmember.CastMemberGateway;
import com.tarcisiolzbraga.codeflix.videos.domain.castmember.CastMemberID;
import com.tarcisiolzbraga.codeflix.videos.domain.castmember.CastMemberType;
import com.tarcisiolzbraga.codeflix.videos.infrastructure.IntegrationTest;
import com.tarcisiolzbraga.codeflix.videos.infrastructure.castmember.CastMemberClient;
import com.tarcisiolzbraga.codeflix.videos.infrastructure.castmember.models.CastMemberDTO;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

// Mensagens de verdade do Debezium, capturadas do tópico adm_videos_mysql.adm_videos.cast_member,
// publicadas num Kafka de verdade. Cada teste troca só o id, para nenhum ver mensagem de outro.
@IntegrationTest
class CastMemberListenerIT {

    private static final Duration TIMEOUT = Duration.ofSeconds(30);
    private static final Duration SETTLE = Duration.ofSeconds(3);

    @Value("${kafka.consumers.cast-member.topics}")
    private String topic;

    @Autowired
    private KafkaTemplate<String, String> testKafkaTemplate;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private CastMemberGateway castMemberGateway;

    @MockitoBean
    private CastMemberClient castMemberClient;

    @Test
    void givenACreateMessage_whenConsumed_thenFetchTheRecordAndStoreTheReplica() {
        final var id = "aaaaaaaa-1111-4111-8111-111111111111";
        when(castMemberClient.castMemberOfId(id)).thenReturn(Optional.of(aDto(id, "Denis Villeneuve", "DIRECTOR")));

        publish("cdc/cast-member-c.json", id);

        await().atMost(TIMEOUT).untilAsserted(() -> {
            final var stored = castMemberGateway.findById(CastMemberID.from(id));
            assertTrue(stored.isPresent());
            assertEquals("Denis Villeneuve", stored.orElseThrow().getName());
            assertEquals(CastMemberType.DIRECTOR, stored.orElseThrow().getType());
        });
    }

    @Test
    void givenAnUpdateMessage_whenConsumed_thenReplaceTheReplicaWithWhatTheAdminHasNow() {
        final var id = "bbbbbbbb-2222-4222-8222-222222222222";
        when(castMemberClient.castMemberOfId(id)).thenReturn(Optional.of(aDto(id, "Denis Villeneuve", "ACTOR")));

        publish("cdc/cast-member-u.json", id);

        // A presença é afirmada antes de ler: orElseThrow() num Optional vazio lança
        // NoSuchElementException, que não é AssertionError, e o Awaitility não repetiria.
        await().atMost(TIMEOUT).untilAsserted(() -> {
            final var stored = castMemberGateway.findById(CastMemberID.from(id));
            assertTrue(stored.isPresent());
            assertEquals(CastMemberType.ACTOR, stored.orElseThrow().getType());
        });
    }

    @Test
    void givenADeleteMessage_whenConsumed_thenRemoveTheReplica() {
        final var id = "cccccccc-3333-4333-8333-333333333333";
        when(castMemberClient.castMemberOfId(id)).thenReturn(Optional.of(aDto(id, "Vai ser apagado", "ACTOR")));
        publish("cdc/cast-member-c.json", id);
        await().atMost(TIMEOUT)
                .untilAsserted(() -> assertTrue(castMemberGateway.findById(CastMemberID.from(id)).isPresent()));

        publish("cdc/cast-member-d.json", id);

        await().atMost(TIMEOUT)
                .untilAsserted(() -> assertTrue(castMemberGateway.findById(CastMemberID.from(id)).isEmpty()));
    }

    @Test
    void givenATombstone_whenConsumed_thenIgnoreItAndKeepTheReplica() {
        final var id = "dddddddd-4444-4444-8444-444444444444";
        when(castMemberClient.castMemberOfId(id)).thenReturn(Optional.of(aDto(id, "Sobrevive", "ACTOR")));
        publish("cdc/cast-member-c.json", id);
        await().atMost(TIMEOUT)
                .untilAsserted(() -> assertTrue(castMemberGateway.findById(CastMemberID.from(id)).isPresent()));

        testKafkaTemplate.send(this.topic, id, null);

        await().during(SETTLE)
                .atMost(TIMEOUT)
                .untilAsserted(() -> assertTrue(castMemberGateway.findById(CastMemberID.from(id)).isPresent()));
    }

    @Test
    void givenAMessageForAMemberTheAdminNoLongerHas_whenConsumed_thenStoreNothing() {
        final var id = "eeeeeeee-5555-4555-8555-555555555555";
        when(castMemberClient.castMemberOfId(id)).thenReturn(Optional.empty());

        publish("cdc/cast-member-c.json", id);

        await().during(SETTLE)
                .atMost(TIMEOUT)
                .untilAsserted(() -> assertFalse(castMemberGateway.findById(CastMemberID.from(id)).isPresent()));
    }

    @Test
    void givenTheAdminReportsATypeWeDoNotKnow_whenConsumed_thenStoreNothing() {
        final var id = "ffffffff-6666-4666-8666-666666666666";
        when(castMemberClient.castMemberOfId(id)).thenReturn(Optional.of(aDto(id, "Tipo estranho", "PRODUTOR")));

        publish("cdc/cast-member-c.json", id);

        await().during(SETTLE)
                .atMost(TIMEOUT)
                .untilAsserted(() -> assertFalse(castMemberGateway.findById(CastMemberID.from(id)).isPresent()));
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

    private static CastMemberDTO aDto(final String id, final String name, final String type) {
        return new CastMemberDTO(
                id,
                name,
                type,
                true,
                Instant.parse("2026-09-30T12:00:00Z"),
                Instant.parse("2026-10-01T08:30:00Z"));
    }
}
