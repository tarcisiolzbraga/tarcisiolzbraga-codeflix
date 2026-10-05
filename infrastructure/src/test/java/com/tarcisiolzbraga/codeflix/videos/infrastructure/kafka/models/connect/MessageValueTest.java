package com.tarcisiolzbraga.codeflix.videos.infrastructure.kafka.models.connect;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tarcisiolzbraga.codeflix.videos.infrastructure.category.models.CategoryEvent;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.core.io.ClassPathResource;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

// As mensagens destes testes não foram inventadas: foram capturadas do tópico
// adm_videos_mysql.adm_videos.category, geradas pelo Debezium 3.6.3 em cima do MySQL do
// admin-codeflix. É o contrato de verdade, não a nossa ideia dele.
@JsonTest
class MessageValueTest {

    private static final TypeReference<MessageValue<CategoryEvent>> CATEGORY_MESSAGE = new TypeReference<>() {};

    @Autowired
    private ObjectMapper mapper;

    @Test
    void givenASnapshotMessage_whenRead_thenSeeOperationReadWithOnlyTheNewState() {
        final var actualPayload = read("cdc/category-r.json");

        assertEquals(Operation.READ, actualPayload.operation().orElseThrow());
        assertTrue(actualPayload.beforeState().isEmpty());
        assertTrue(actualPayload.afterState().isPresent());
    }

    @Test
    void givenACreateMessage_whenRead_thenSeeOperationCreateAndTheNewId() {
        final var actualPayload = read("cdc/category-c.json");

        assertEquals(Operation.CREATE, actualPayload.operation().orElseThrow());
        assertEquals("00000000-cdc0-4000-8000-000000000001", actualPayload.afterState().orElseThrow().id());
        assertTrue(actualPayload.beforeState().isEmpty());
    }

    @Test
    void givenAnUpdateMessage_whenRead_thenSeeBothStates() {
        final var actualPayload = read("cdc/category-u.json");

        assertEquals(Operation.UPDATE, actualPayload.operation().orElseThrow());
        assertTrue(actualPayload.beforeState().isPresent());
        assertTrue(actualPayload.afterState().isPresent());
        assertEquals(
                actualPayload.beforeState().orElseThrow().id(),
                actualPayload.afterState().orElseThrow().id());
    }

    @Test
    void givenADeleteMessage_whenRead_thenSeeOnlyTheOldState() {
        final var actualPayload = read("cdc/category-d.json");

        assertEquals(Operation.DELETE, actualPayload.operation().orElseThrow());
        assertTrue(actualPayload.operation().orElseThrow().isDelete());
        assertEquals("00000000-cdc0-4000-8000-000000000001", actualPayload.beforeState().orElseThrow().id());
        assertTrue(actualPayload.afterState().isEmpty());
    }

    @Test
    void givenAnyMessage_whenRead_thenSeeWhereItCameFrom() {
        final var actualSource = read("cdc/category-u.json").source();

        assertEquals("mysql", actualSource.connector());
        assertEquals("adm_videos_mysql", actualSource.name());
        assertEquals("adm_videos", actualSource.database());
        assertEquals("category", actualSource.table());
    }

    @Test
    void givenAMessageWithFieldsWeDoNotMap_whenRead_thenIgnoreThemInsteadOfFailing() {
        final var actualPayload = read("cdc/category-u.json");

        assertEquals(Operation.UPDATE, actualPayload.operation().orElseThrow());
    }

    @Test
    void givenAnUnknownOperation_whenRead_thenLeaveItEmptyInsteadOfGuessing() throws Exception {
        final var json = """
                {"payload":{"before":null,"after":null,"source":null,"op":"xyz"}}""";

        final var actualPayload = mapper.readValue(json, CATEGORY_MESSAGE).payload();

        assertTrue(actualPayload.operation().isEmpty());
    }

    @Test
    void givenATruncateOperation_whenRead_thenRecognizeItAndSeeItCarriesNoState() throws Exception {
        final var json = """
                {"payload":{"before":null,"after":null,"source":null,"op":"t"}}""";

        final var actualPayload = mapper.readValue(json, CATEGORY_MESSAGE).payload();

        assertEquals(Operation.TRUNCATE, actualPayload.operation().orElseThrow());
        assertTrue(actualPayload.afterState().isEmpty());
        assertEquals(false, actualPayload.operation().orElseThrow().carriesNewState());
    }

    private ValuePayload<CategoryEvent> read(final String resource) {
        try {
            final var json = new String(
                    new ClassPathResource(resource).getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            return this.mapper.readValue(json, CATEGORY_MESSAGE).payload();
        } catch (final Exception e) {
            throw new IllegalStateException("não foi possível ler " + resource, e);
        }
    }
}
