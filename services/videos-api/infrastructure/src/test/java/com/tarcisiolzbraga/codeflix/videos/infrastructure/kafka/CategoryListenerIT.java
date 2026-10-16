package com.tarcisiolzbraga.codeflix.videos.infrastructure.kafka;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.tarcisiolzbraga.codeflix.videos.domain.category.CategoryGateway;
import com.tarcisiolzbraga.codeflix.videos.domain.category.CategoryID;
import com.tarcisiolzbraga.codeflix.videos.infrastructure.IntegrationTest;
import com.tarcisiolzbraga.codeflix.videos.infrastructure.category.CategoryClient;
import com.tarcisiolzbraga.codeflix.videos.infrastructure.category.models.CategoryDTO;
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

// Exercita o caminho inteiro de chegada: mensagem de verdade do Debezium publicada num Kafka de
// verdade, o listener a consome e a réplica aparece no Elasticsearch.
//
// As mensagens vêm das fixtures capturadas do tópico do admin-codeflix, com o id trocado por um
// próprio de cada teste: a estrutura segue sendo a que o Debezium 3.6.3 produz, e nenhum teste vê
// mensagem de outro, o que é a origem clássica de intermitência num consumidor compartilhado.
//
// O CategoryClient é mockado de propósito: ele fala com a API do admin, que não sobe aqui, e o que
// este teste tem a dizer é se o listener roteia cada operação para o caso de uso certo.
@IntegrationTest
class CategoryListenerIT {

    private static final Duration TIMEOUT = Duration.ofSeconds(30);
    private static final Duration SETTLE = Duration.ofSeconds(3);

    @Value("${kafka.consumers.category.topics}")
    private String topic;

    @Autowired
    private KafkaTemplate<String, String> testKafkaTemplate;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private CategoryGateway categoryGateway;

    @MockitoBean
    private CategoryClient categoryClient;

    @Test
    void givenACreateMessage_whenConsumed_thenFetchTheRecordAndStoreTheReplica() {
        final var id = "11111111-1111-4111-8111-111111111111";
        when(categoryClient.categoryOfId(id)).thenReturn(Optional.of(aDto(id, "Vinda do create")));

        publish("cdc/category-c.json", id);

        await().atMost(TIMEOUT).untilAsserted(() -> {
            final var stored = categoryGateway.findById(CategoryID.from(id));
            assertTrue(stored.isPresent());
            assertEquals("Vinda do create", stored.orElseThrow().getName());
        });
    }

    @Test
    void givenASnapshotMessage_whenConsumed_thenStoreTheReplicaJustTheSame() {
        final var id = "22222222-2222-4222-8222-222222222222";
        when(categoryClient.categoryOfId(id)).thenReturn(Optional.of(aDto(id, "Vinda do snapshot")));

        publish("cdc/category-r.json", id);

        await().atMost(TIMEOUT)
                .untilAsserted(() -> assertTrue(categoryGateway.findById(CategoryID.from(id)).isPresent()));
    }

    @Test
    void givenAnUpdateMessage_whenConsumed_thenReplaceTheReplicaWithWhatTheAdminHasNow() {
        final var id = "33333333-3333-4333-8333-333333333333";
        when(categoryClient.categoryOfId(id)).thenReturn(Optional.of(aDto(id, "Nome atualizado")));

        publish("cdc/category-u.json", id);

        // A presença é afirmada antes de ler: orElseThrow() num Optional vazio lança
        // NoSuchElementException, que não é AssertionError, e o Awaitility não repetiria.
        await().atMost(TIMEOUT).untilAsserted(() -> {
            final var stored = categoryGateway.findById(CategoryID.from(id));
            assertTrue(stored.isPresent());
            assertEquals("Nome atualizado", stored.orElseThrow().getName());
        });
    }

    @Test
    void givenADeleteMessage_whenConsumed_thenRemoveTheReplica() {
        final var id = "44444444-4444-4444-8444-444444444444";
        when(categoryClient.categoryOfId(id)).thenReturn(Optional.of(aDto(id, "Vai ser apagada")));
        publish("cdc/category-c.json", id);
        await().atMost(TIMEOUT)
                .untilAsserted(() -> assertTrue(categoryGateway.findById(CategoryID.from(id)).isPresent()));

        publish("cdc/category-d.json", id);

        await().atMost(TIMEOUT)
                .untilAsserted(() -> assertTrue(categoryGateway.findById(CategoryID.from(id)).isEmpty()));
    }

    @Test
    void givenATombstone_whenConsumed_thenIgnoreItAndKeepTheReplica() {
        final var id = "55555555-5555-4555-8555-555555555555";
        when(categoryClient.categoryOfId(id)).thenReturn(Optional.of(aDto(id, "Sobrevive ao tombstone")));
        publish("cdc/category-c.json", id);
        await().atMost(TIMEOUT)
                .untilAsserted(() -> assertTrue(categoryGateway.findById(CategoryID.from(id)).isPresent()));

        testKafkaTemplate.send(this.topic, id, null);

        await().during(SETTLE)
                .atMost(TIMEOUT)
                .untilAsserted(() -> assertTrue(categoryGateway.findById(CategoryID.from(id)).isPresent()));
    }

    @Test
    void givenAMessageForACategoryTheAdminNoLongerHas_whenConsumed_thenStoreNothing() {
        final var id = "66666666-6666-4666-8666-666666666666";
        when(categoryClient.categoryOfId(id)).thenReturn(Optional.empty());

        publish("cdc/category-c.json", id);

        await().during(SETTLE)
                .atMost(TIMEOUT)
                .untilAsserted(() -> assertFalse(categoryGateway.findById(CategoryID.from(id)).isPresent()));
    }

    // Lê a fixture capturada e troca só o id, em before e after, preservando todo o resto do
    // envelope que o Debezium produz.
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

    private static CategoryDTO aDto(final String id, final String name) {
        return new CategoryDTO(
                id,
                name,
                "vinda da API do admin",
                true,
                Instant.parse("2026-09-30T12:00:00Z"),
                Instant.parse("2026-10-01T08:30:00Z"));
    }
}
