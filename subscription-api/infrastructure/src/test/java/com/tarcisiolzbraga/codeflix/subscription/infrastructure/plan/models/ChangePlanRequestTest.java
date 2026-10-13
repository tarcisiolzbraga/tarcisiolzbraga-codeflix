package com.tarcisiolzbraga.codeflix.subscription.infrastructure.plan.models;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;

@JsonTest
class ChangePlanRequestTest {

    @Autowired
    private JacksonTester<ChangePlanRequest> json;

    @Test
    void givenACompleteBody_whenReads_thenEveryFieldArrives() throws IOException {
        final var body = """
                {"name":"Básico","description":"Uma tela","currency":"BRL","price":19.90}""";

        final var actual = this.json.parseObject(body);

        assertEquals("Básico", actual.name());
        assertEquals("Uma tela", actual.description());
        assertEquals("BRL", actual.currency());
        assertEquals(0, actual.price().compareTo(new BigDecimal("19.90")));
    }

    // O corpo de alteração não tem `active`, e um cliente que o mandar não muda estado nenhum: a
    // ativação tem rotas próprias. O campo é ignorado na leitura, e este teste é o registro disso.
    @Test
    void givenABodyCarryingActive_whenReads_thenTheFieldIsIgnoredBecauseItIsNotInTheContract() throws IOException {
        final var body = """
                {"name":"Básico","description":"Uma tela","currency":"BRL","price":19.90,"active":true}""";

        final var actual = this.json.parseObject(body);

        assertEquals("Básico", actual.name());
    }
}
