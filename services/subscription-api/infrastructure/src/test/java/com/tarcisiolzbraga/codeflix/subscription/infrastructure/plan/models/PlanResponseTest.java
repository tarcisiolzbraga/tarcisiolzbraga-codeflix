package com.tarcisiolzbraga.codeflix.subscription.infrastructure.plan.models;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.test.json.JsonContent;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

// Aqui mora o contrato público da API: o teste fixa o conjunto exato de campos, para pegar rename,
// campo a mais e campo a menos. Só a escrita, que é como a aplicação usa um response.
@JsonTest
class PlanResponseTest {

    private static final Set<String> EXPECTED_FIELDS =
            Set.of("id", "name", "description", "active", "currency", "price", "createdAt", "updatedAt");
    private static final Instant CREATED_AT = Instant.parse("2026-01-31T10:15:30.123456Z");
    private static final Instant UPDATED_AT = Instant.parse("2026-02-01T11:16:31.654321Z");

    @Autowired
    private JacksonTester<PlanResponse> json;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void givenAResponse_whenSerializes_thenWritesExactlyTheFieldsOfTheContract() throws IOException {
        final var response = responseFixture();

        final var actual = this.json.write(response);

        assertEquals(EXPECTED_FIELDS, fieldsOf(actual).keySet());
    }

    @Test
    void givenAResponse_whenSerializes_thenKeepsTheValuesWithIsoDates() throws IOException {
        final var response = responseFixture();

        final var actual = fieldsOf(this.json.write(response));

        assertEquals("11111111-1111-1111-1111-111111111111", actual.get("id"));
        assertEquals("Plus", actual.get("name"));
        assertEquals(true, actual.get("active"));
        assertEquals(CREATED_AT.toString(), actual.get("createdAt"));
        assertEquals(UPDATED_AT.toString(), actual.get("updatedAt"));
    }

    // O preço é número no JSON, e os centavos têm de atravessar inteiros.
    @Test
    void givenAPriceWithCents_whenSerializes_thenTheAmountSurvives() throws IOException {
        final var response = responseFixture();

        final var actual = fieldsOf(this.json.write(response));

        assertEquals(0, new BigDecimal(String.valueOf(actual.get("price"))).compareTo(new BigDecimal("49.90")));
    }

    @Test
    void givenAResponse_whenSerializes_thenTheCurrencyIsTheThreeLetterCode() throws IOException {
        final var response = responseFixture();

        final var actual = fieldsOf(this.json.write(response));

        assertEquals("BRL", actual.get("currency"));
    }

    private PlanResponse responseFixture() {
        return new PlanResponse(
                "11111111-1111-1111-1111-111111111111",
                "Plus",
                "Quatro telas",
                true,
                "BRL",
                new BigDecimal("49.90"),
                CREATED_AT,
                UPDATED_AT);
    }

    private Map<String, Object> fieldsOf(final JsonContent<PlanResponse> content) throws IOException {
        return this.objectMapper.readValue(content.getJson(), new TypeReference<>() {});
    }
}
