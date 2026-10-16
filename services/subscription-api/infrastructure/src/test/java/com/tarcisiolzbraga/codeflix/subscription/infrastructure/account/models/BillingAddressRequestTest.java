package com.tarcisiolzbraga.codeflix.subscription.infrastructure.account.models;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;

@JsonTest
class BillingAddressRequestTest {

    @Autowired
    private JacksonTester<BillingAddressRequest> json;

    @Test
    void givenACompleteBody_whenReads_thenEveryFieldArrives() throws IOException {
        final var body =
                """
                {"zipcode":"01310930","number":"1578","complement":"apto 42","country":"BR"}""";

        final var actual = this.json.parseObject(body);

        assertEquals("01310930", actual.zipcode());
        assertEquals("1578", actual.number());
        assertEquals("apto 42", actual.complement());
        assertEquals("BR", actual.country());
    }

    // O complemento é o único opcional: nem todo endereço tem um.
    @Test
    void givenNoComplement_whenReads_thenItArrivesNullAndTheRestIsThere() throws IOException {
        final var body = """
                {"zipcode":"01310930","number":"1578","country":"BR"}""";

        final var actual = this.json.parseObject(body);

        assertNull(actual.complement());
        assertEquals("1578", actual.number());
    }

    // O corpo não carrega id de conta: quem chama muda o próprio endereço, e quem ele é vem do
    // token. Um campo desses no JSON é ignorado, e este teste é o registro disso.
    @Test
    void givenABodyCarryingAnAccountId_whenReads_thenItIsIgnoredBecauseItIsNotInTheContract() throws IOException {
        final var body =
                """
                {"zipcode":"01310930","number":"1578","country":"BR","accountId":"de-outra-pessoa"}""";

        final var actual = this.json.parseObject(body);

        assertEquals("01310930", actual.zipcode());
    }
}
