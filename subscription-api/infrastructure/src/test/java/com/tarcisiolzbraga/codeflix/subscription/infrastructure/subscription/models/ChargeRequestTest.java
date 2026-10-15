package com.tarcisiolzbraga.codeflix.subscription.infrastructure.subscription.models;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;

@JsonTest
class ChargeRequestTest {

    @Autowired
    private JacksonTester<ChargeRequest> json;

    @Test
    void givenABodyWithTheCardToken_whenReads_thenItArrives() throws IOException {
        final var body = "{\"creditCardToken\":\"tok_visa_4242\"}";

        final var actual = this.json.parseObject(body);

        assertEquals("tok_visa_4242", actual.creditCardToken());
    }

    // Número e CVV não têm campo onde entrar, e é de propósito: dado de cartão que não chega aqui
    // não vaza daqui, e não põe o serviço no escopo do PCI DSS.
    @Test
    void givenABodyWithRawCardData_whenReads_thenOnlyTheTokenArrives() throws IOException {
        final var body =
                """
                {"creditCardToken":"tok_visa_4242","number":"4242424242424242","cvv":"123"}""";

        final var actual = this.json.parseObject(body);

        assertEquals("tok_visa_4242", actual.creditCardToken());
    }
}
