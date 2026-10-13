package com.tarcisiolzbraga.codeflix.subscription.infrastructure.plan.models;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;

// Só a leitura, que é como a aplicação usa um request.
@JsonTest
class CreatePlanRequestTest {

    private static final String FULL_BODY =
            """
            {"name":"Plus","description":"Quatro telas","active":true,"currency":"BRL","price":49.90}""";

    @Autowired
    private JacksonTester<CreatePlanRequest> json;

    @Test
    void givenACompleteBody_whenReads_thenEveryFieldArrives() throws IOException {
        final var body = FULL_BODY;

        final var actual = this.json.parseObject(body);

        assertEquals("Plus", actual.name());
        assertEquals("Quatro telas", actual.description());
        assertEquals("BRL", actual.currency());
    }

    @Test
    void givenACompleteBody_whenReads_thenThePriceKeepsItsCents() throws IOException {
        final var body = FULL_BODY;

        final var actual = this.json.parseObject(body);

        assertEquals(0, actual.price().compareTo(new BigDecimal("49.90")));
    }

    @Test
    void givenActiveTrue_whenReads_thenTheRequestSaysActive() throws IOException {
        final var body = FULL_BODY;

        final var actual = this.json.parseObject(body);

        assertTrue(actual.isActive());
    }

    // Ausente significa inativo: pôr um plano à venda é ato deliberado.
    @Test
    void givenNoActiveField_whenReads_thenTheRequestSaysInactive() throws IOException {
        final var body = """
                {"name":"Plus","description":"Quatro telas","currency":"BRL","price":49.90}""";

        final var actual = this.json.parseObject(body);

        assertNull(actual.active());
        assertFalse(actual.isActive());
    }
}
