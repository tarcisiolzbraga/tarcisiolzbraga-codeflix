package com.tarcisiolzbraga.codeflix.subscription.infrastructure.account.models;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;

// Só a leitura, que é como a aplicação usa um request.
@JsonTest
class SignUpRequestTest {

    @Autowired
    private JacksonTester<SignUpRequest> json;

    @Test
    void givenACompleteBody_whenReads_thenEveryFieldArrives() throws IOException {
        final var body =
                """
                {"firstname":"Tarcisio","lastname":"Braga","email":"assinante@codeflix.com.br",\
                "password":"uma-senha-boa","documentNumber":"11144477735","documentType":"cpf"}""";

        final var actual = this.json.parseObject(body);

        assertEquals("Tarcisio", actual.firstname());
        assertEquals("Braga", actual.lastname());
        assertEquals("assinante@codeflix.com.br", actual.email());
        assertEquals("uma-senha-boa", actual.password());
        assertEquals("11144477735", actual.documentNumber());
        assertEquals("cpf", actual.documentType());
    }
}
