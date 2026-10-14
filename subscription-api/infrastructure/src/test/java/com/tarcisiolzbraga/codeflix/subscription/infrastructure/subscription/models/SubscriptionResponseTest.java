package com.tarcisiolzbraga.codeflix.subscription.infrastructure.subscription.models;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tarcisiolzbraga.codeflix.subscription.application.subscription.SubscriptionOutput;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

// Aqui mora o contrato público: o teste fixa o conjunto exato de campos, para pegar rename, campo a
// mais e campo a menos. Só a escrita, que é como a aplicação usa um response.
@JsonTest
class SubscriptionResponseTest {

    private static final Set<String> EXPECTED_FIELDS =
            Set.of("id", "planId", "status", "dueDate", "lastRenewDate", "createdAt");
    private static final String ID = "11111111-1111-1111-1111-111111111111";
    private static final String PLAN_ID = "22222222-2222-2222-2222-222222222222";

    @Autowired
    private JacksonTester<SubscriptionResponse> json;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void givenAResponse_whenSerializes_thenWritesExactlyTheFieldsOfTheContract() throws IOException {
        final var actual = this.json.write(responseWith(Instant.parse("2026-10-14T12:00:00Z")));

        assertEquals(EXPECTED_FIELDS, fieldsOf(actual.getJson()).keySet());
    }

    // A data de vencimento sai como data, sem hora e sem fuso: é por dia que se decide se venceu, e
    // mandar um instante convidaria o cliente a convertê-lo para o fuso dele e errar o dia.
    @Test
    void givenAResponse_whenSerializes_thenTheDueDateIsAPlainDate() throws IOException {
        final var actual = fieldsOf(this.json.write(responseWith(null)).getJson());

        assertEquals("2026-11-14", actual.get("dueDate"));
        assertEquals("trailing", actual.get("status"));
        assertEquals(ID, actual.get("id"));
        assertEquals(PLAN_ID, actual.get("planId"));
    }

    // Assinatura que nunca foi renovada sai com o campo nulo, e não ausente: a ausência é
    // informação, e o cliente precisa distinguir "nunca renovou" de "o campo mudou de nome".
    @Test
    void givenASubscriptionNeverRenewed_whenSerializes_thenTheFieldIsNullAndStillPresent() throws IOException {
        final var actual = fieldsOf(this.json.write(responseWith(null)).getJson());

        assertTrue(actual.containsKey("lastRenewDate"));
        assertNull(actual.get("lastRenewDate"));
    }

    // Nada de versão no corpo: o número do travamento otimista é detalhe de como o banco protege a
    // escrita, não assunto de quem lê a própria assinatura.
    @Test
    void givenAResponse_whenSerializes_thenThereIsNoVersionAndNoAccountId() throws IOException {
        final var actual = this.json.write(responseWith(null)).getJson();

        assertTrue(!actual.contains("version") && !actual.contains("accountId"));
    }

    private static SubscriptionResponse responseWith(final Instant lastRenewDate) {
        return SubscriptionResponse.from(new SubscriptionOutput(
                ID,
                PLAN_ID,
                "trailing",
                LocalDate.parse("2026-11-14"),
                lastRenewDate,
                Instant.parse("2026-10-14T12:00:00Z")));
    }

    private Map<String, Object> fieldsOf(final String content) throws IOException {
        return this.objectMapper.readValue(content, new TypeReference<>() {});
    }
}
