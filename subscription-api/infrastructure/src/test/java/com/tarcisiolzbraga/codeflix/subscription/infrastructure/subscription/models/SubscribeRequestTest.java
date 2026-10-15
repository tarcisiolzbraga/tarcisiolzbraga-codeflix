package com.tarcisiolzbraga.codeflix.subscription.infrastructure.subscription.models;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;

@JsonTest
class SubscribeRequestTest {

    @Autowired
    private JacksonTester<SubscribeRequest> json;

    @Test
    void givenABodyWithThePlan_whenReads_thenItArrives() throws IOException {
        final var body = "{\"planId\":\"11111111-1111-1111-1111-111111111111\"}";

        final var actual = this.json.parseObject(body);

        assertEquals("11111111-1111-1111-1111-111111111111", actual.planId());
    }

    // Quem assina vem do token, nunca do corpo: um campo userId aqui deixaria alguém assinar em
    // nome de outro. Se um dia aparecer, o corpo o ignora — e este teste prova que ignora.
    @Test
    void givenABodyTryingToChooseTheSubscriber_whenReads_thenOnlyThePlanArrives() throws IOException {
        final var body =
                """
                {"planId":"11111111-1111-1111-1111-111111111111","userId":"de-outra-pessoa"}""";

        final var actual = this.json.parseObject(body);

        assertEquals("11111111-1111-1111-1111-111111111111", actual.planId());
    }

    @Test
    void givenAnEmptyBody_whenReads_thenThePlanComesNullAndTheUseCaseRefusesItLater() throws IOException {
        final var actual = this.json.parseObject("{}");

        assertNull(actual.planId());
    }
}
