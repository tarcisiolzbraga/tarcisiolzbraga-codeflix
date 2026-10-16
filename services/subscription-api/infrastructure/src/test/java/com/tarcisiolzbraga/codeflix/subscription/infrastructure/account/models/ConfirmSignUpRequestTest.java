package com.tarcisiolzbraga.codeflix.subscription.infrastructure.account.models;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;

@JsonTest
class ConfirmSignUpRequestTest {

    @Autowired
    private JacksonTester<ConfirmSignUpRequest> json;

    // O token vem no corpo, não na URL: em query string ele acabaria no log do servidor e no
    // histórico do navegador, e ele é credencial.
    @Test
    void givenABodyWithTheToken_whenReads_thenItArrives() throws IOException {
        final var body = "{\"token\":\"abc-123_XYZ\"}";

        final var actual = this.json.parseObject(body);

        assertEquals("abc-123_XYZ", actual.token());
    }
}
