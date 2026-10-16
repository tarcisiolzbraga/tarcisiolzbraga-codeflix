package com.tarcisiolzbraga.codeflix.subscription.infrastructure.account.models;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tarcisiolzbraga.codeflix.subscription.infrastructure.account.models.AccountResponse.BillingAddressResponse;
import java.io.IOException;
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
class AccountResponseTest {

    private static final Set<String> EXPECTED_FIELDS = Set.of(
            "id",
            "email",
            "firstname",
            "lastname",
            "documentNumber",
            "documentType",
            "billingAddress",
            "hasPaymentMethod");
    private static final Set<String> EXPECTED_ADDRESS_FIELDS =
            Set.of("zipcode", "number", "complement", "country");

    @Autowired
    private JacksonTester<AccountResponse> json;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void givenAResponse_whenSerializes_thenWritesExactlyTheFieldsOfTheContract() throws IOException {
        final var response = responseWith(anAddress());

        final var actual = this.json.write(response);

        assertEquals(EXPECTED_FIELDS, fieldsOf(actual.getJson()).keySet());
    }

    @Test
    void givenAResponse_whenSerializes_thenKeepsTheValues() throws IOException {
        final var response = responseWith(anAddress());

        final var actual = fieldsOf(this.json.write(response).getJson());

        assertEquals("11111111-1111-1111-1111-111111111111", actual.get("id"));
        assertEquals("assinante@codeflix.com.br", actual.get("email"));
        assertEquals("11144477735", actual.get("documentNumber"));
        assertEquals("cpf", actual.get("documentType"));
    }

    @Test
    void givenAnAddress_whenSerializes_thenTheNestedObjectHasItsOwnExactFields() throws IOException {
        final var response = responseWith(anAddress());

        final var actual = fieldsOf(this.json.write(response).getJson());

        @SuppressWarnings("unchecked")
        final var address = (Map<String, Object>) actual.get("billingAddress");
        assertEquals(EXPECTED_ADDRESS_FIELDS, address.keySet());
        assertEquals("01310930", address.get("zipcode"));
    }

    // Conta sem endereço sai com o campo nulo, e não com um objeto de campos vazios: a ausência é
    // informação, e o front precisa distinguir "não informou" de "informou vazio".
    @Test
    void givenNoAddress_whenSerializes_thenTheFieldIsNullAndStillPresent() throws IOException {
        final var response = responseWith(null);

        final var actual = fieldsOf(this.json.write(response).getJson());

        assertTrue(actual.containsKey("billingAddress"));
        assertNull(actual.get("billingAddress"));
    }

    // Nada de senha nem de id do provedor no corpo: a primeira nunca sai daqui, o segundo é detalhe
    // de como a autenticação funciona.
    @Test
    void givenAResponse_whenSerializes_thenThereIsNoPasswordAndNoProviderId() throws IOException {
        final var response = responseWith(anAddress());

        final var actual = this.json.write(response).getJson();

        assertTrue(!actual.contains("password") && !actual.contains("userId") && !actual.contains("idp"));
    }

    // O front precisa saber se tem de pedir cartão, e é só isso que sai: **a referência do meio de
    // pagamento não aparece no JSON**. Ela é um identificador do provedor e não serve a quem chama.
    @Test
    void givenAnAccountWithAStoredPaymentMethod_whenSerializes_thenOnlyTheFlagComesOut() throws IOException {
        final var response = responseWith(anAddress(), true);

        final var json = this.json.write(response).getJson();

        assertEquals(Boolean.TRUE, fieldsOf(json).get("hasPaymentMethod"));
        assertTrue(
                !json.contains("providerRef") && !json.contains("paymentMethodRef") && !json.contains("pm_"),
                "a referência do provedor não deveria sair no corpo: " + json);
    }

    private static AccountResponse responseWith(final BillingAddressResponse address) {
        return responseWith(address, false);
    }

    private static AccountResponse responseWith(
            final BillingAddressResponse address, final boolean hasPaymentMethod) {
        return new AccountResponse(
                "11111111-1111-1111-1111-111111111111",
                "assinante@codeflix.com.br",
                "Tarcisio",
                "Braga",
                "11144477735",
                "cpf",
                address,
                hasPaymentMethod);
    }

    private static BillingAddressResponse anAddress() {
        return new BillingAddressResponse("01310930", "1578", "apto 42", "BR");
    }

    private Map<String, Object> fieldsOf(final String content) throws IOException {
        return this.objectMapper.readValue(content, new TypeReference<>() {});
    }
}
