package com.tarcisiolzbraga.codeflix.subscription.infrastructure.subscription.models;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tarcisiolzbraga.codeflix.subscription.application.subscription.SubscriptionOutput;
import com.tarcisiolzbraga.codeflix.subscription.application.subscription.charge.ChargeSubscriptionOutput;
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

@JsonTest
class ChargeResponseTest {

    private static final Set<String> EXPECTED_FIELDS = Set.of("subscription", "charged", "transactionId", "reason");
    private static final Set<String> EXPECTED_SUBSCRIPTION_FIELDS =
            Set.of("id", "planId", "status", "dueDate", "lastRenewDate", "createdAt");

    @Autowired
    private JacksonTester<ChargeResponse> json;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void givenAResponse_whenSerializes_thenWritesExactlyTheFieldsOfTheContract() throws IOException {
        final var actual = fieldsOf(this.json.write(ChargeResponse.from(succeeded("tx-1"))).getJson());

        assertEquals(EXPECTED_FIELDS, actual.keySet());
    }

    @Test
    void givenAChargeThatPassed_whenSerializes_thenCarriesTheTransactionAndNoReason() throws IOException {
        final var actual = fieldsOf(this.json.write(ChargeResponse.from(succeeded("tx-1"))).getJson());

        assertEquals(Boolean.TRUE, actual.get("charged"));
        assertEquals("tx-1", actual.get("transactionId"));
        assertNull(actual.get("reason"));
    }

    // O motivo da recusa é o campo mais importante da resposta: é o que o cliente mostra para a
    // pessoa saber que tem de trocar o cartão.
    @Test
    void givenAChargeThatWasRefused_whenSerializes_thenCarriesTheReason() throws IOException {
        final var response =
                new ChargeResponse(subscriptionResponse("incomplete"), false, "tx-1", "the card was refused");

        final var actual = fieldsOf(this.json.write(response).getJson());

        assertEquals(Boolean.FALSE, actual.get("charged"));
        assertEquals("the card was refused", actual.get("reason"));
    }

    // Provedor fora do ar não abre transação: o id vem nulo, e o campo continua presente.
    @Test
    void givenARefusalWithoutTransaction_whenSerializes_thenTheIdIsNullAndStillPresent() throws IOException {
        final var response = new ChargeResponse(
                subscriptionResponse("incomplete"), false, null, "the payment provider could not be reached");

        final var actual = fieldsOf(this.json.write(response).getJson());

        assertTrue(actual.containsKey("transactionId"));
        assertNull(actual.get("transactionId"));
    }

    @Test
    void givenAResponse_whenSerializes_thenTheNestedSubscriptionHasItsOwnExactFields() throws IOException {
        final var actual = fieldsOf(this.json.write(ChargeResponse.from(succeeded("tx-1"))).getJson());

        @SuppressWarnings("unchecked")
        final var subscription = (Map<String, Object>) actual.get("subscription");
        assertEquals(EXPECTED_SUBSCRIPTION_FIELDS, subscription.keySet());
        assertEquals("active", subscription.get("status"));
    }

    private static ChargeSubscriptionOutput succeeded(final String transactionId) {
        return new ChargeSubscriptionOutput(anOutput("active"), true, transactionId, null);
    }

    private static SubscriptionResponse subscriptionResponse(final String status) {
        return SubscriptionResponse.from(anOutput(status));
    }

    private static SubscriptionOutput anOutput(final String status) {
        return new SubscriptionOutput(
                "11111111-1111-1111-1111-111111111111",
                "22222222-2222-2222-2222-222222222222",
                status,
                LocalDate.parse("2026-11-14"),
                Instant.parse("2026-10-14T12:00:00Z"),
                Instant.parse("2026-10-14T12:00:00Z"));
    }

    private Map<String, Object> fieldsOf(final String content) throws IOException {
        return this.objectMapper.readValue(content, new TypeReference<>() {});
    }
}
