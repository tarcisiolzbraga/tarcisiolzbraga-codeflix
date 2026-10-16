package com.tarcisiolzbraga.codeflix.subscription.domain.person;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.tarcisiolzbraga.codeflix.subscription.domain.exceptions.DomainException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class DocumentTest {

    // Números válidos de verdade: os dígitos verificadores fecham. São os usados em teste no Brasil.
    private static final String VALID_CPF = "11144477735";
    private static final String VALID_CNPJ = "11222333000181";

    // Números válidos de outras fontes, para a conta dos dígitos não ficar afirmada por um caso só.
    @ParameterizedTest
    @ValueSource(strings = {"11144477735", "52998224725", "12345678909"})
    void givenOtherValidCpfs_whenCreates_thenTheyAreAccepted(final String cpf) {
        final var actual = new Document.Cpf(cpf);

        assertEquals(cpf, actual.value());
    }

    @ParameterizedTest
    @ValueSource(strings = {"11222333000181", "11444777000161"})
    void givenOtherValidCnpjs_whenCreates_thenTheyAreAccepted(final String cnpj) {
        final var actual = new Document.Cnpj(cnpj);

        assertEquals(cnpj, actual.value());
    }

    // Cada um destes é um válido com um dígito trocado: a conta tem de recusar todos.
    @ParameterizedTest
    @ValueSource(strings = {"11144477734", "52998224726", "12345678900", "11144477745"})
    void givenCpfsOffByOneDigit_whenCreates_thenTheyAreRefused(final String cpf) {
        final var actual = assertThrows(DomainException.class, () -> new Document.Cpf(cpf));

        assertEquals("'cpf' is invalid", actual.getMessage());
    }

    @Test
    void givenAValidCpf_whenCreates_thenHoldsIt() {
        final var actual = new Document.Cpf(VALID_CPF);

        final var value = actual.value();

        assertEquals(VALID_CPF, value);
    }

    @Test
    void givenAValidCpf_whenAsksItsType_thenItSaysCpf() {
        final var actual = new Document.Cpf(VALID_CPF);

        final var type = actual.type();

        assertEquals("cpf", type);
    }

    // O curso valida só o comprimento, então este passava por ele.
    @Test
    void givenACpfWithTheWrongCheckDigit_whenCreates_thenThrowsDomainException() {
        final var invalid = "11144477734";

        final var actual = assertThrows(DomainException.class, () -> new Document.Cpf(invalid));

        assertEquals("'cpf' is invalid", actual.getMessage());
    }

    // Todos os dígitos iguais fecham a conta por acidente, e não são CPF de ninguém.
    @Test
    void givenACpfOfRepeatedDigits_whenCreates_thenThrowsDomainException() {
        final var invalid = "11111111111";

        final var actual = assertThrows(DomainException.class, () -> new Document.Cpf(invalid));

        assertEquals("'cpf' is invalid", actual.getMessage());
    }

    @Test
    void givenACpfWithFewerDigits_whenCreates_thenSaysHowManyItNeeds() {
        final var invalid = "1114447773";

        final var actual = assertThrows(DomainException.class, () -> new Document.Cpf(invalid));

        assertEquals("'cpf' should have 11 digits", actual.getMessage());
    }

    @Test
    void givenACpfWithPunctuation_whenCreates_thenSaysHowManyItNeeds() {
        final var invalid = "111.444.777-35";

        final var actual = assertThrows(DomainException.class, () -> new Document.Cpf(invalid));

        assertEquals("'cpf' should have 11 digits", actual.getMessage());
    }

    @Test
    void givenABlankCpf_whenCreates_thenThrowsDomainException() {
        final var invalid = "  ";

        final var actual = assertThrows(DomainException.class, () -> new Document.Cpf(invalid));

        assertEquals("'cpf' should not be empty", actual.getMessage());
    }

    @Test
    void givenAValidCnpj_whenCreates_thenHoldsItAndSaysItsType() {
        final var actual = new Document.Cnpj(VALID_CNPJ);

        final var type = actual.type();

        assertEquals("cnpj", type);
    }

    @Test
    void givenACnpjWithTheWrongCheckDigit_whenCreates_thenThrowsDomainException() {
        final var invalid = "11222333000182";

        final var actual = assertThrows(DomainException.class, () -> new Document.Cnpj(invalid));

        assertEquals("'cnpj' is invalid", actual.getMessage());
    }

    @Test
    void givenACnpjOfRepeatedDigits_whenCreates_thenThrowsDomainException() {
        final var invalid = "00000000000000";

        final var actual = assertThrows(DomainException.class, () -> new Document.Cnpj(invalid));

        assertEquals("'cnpj' is invalid", actual.getMessage());
    }

    @Test
    void givenACnpjWithFewerDigits_whenCreates_thenSaysHowManyItNeeds() {
        final var invalid = "1122233300018";

        final var actual = assertThrows(DomainException.class, () -> new Document.Cnpj(invalid));

        assertEquals("'cnpj' should have 14 digits", actual.getMessage());
    }

    @Test
    void givenTheCpfType_whenCreatesFromTheFactory_thenBuildsACpf() {
        final var actual = Document.create(VALID_CPF, "cpf");

        assertInstanceOf(Document.Cpf.class, actual);
    }

    @Test
    void givenTheCnpjType_whenCreatesFromTheFactory_thenBuildsACnpj() {
        final var actual = Document.create(VALID_CNPJ, "cnpj");

        assertInstanceOf(Document.Cnpj.class, actual);
    }

    // A referência diz só "Invalid document type", sem dizer qual veio.
    @Test
    void givenAnUnknownType_whenCreatesFromTheFactory_thenSaysWhichTypeCame() {
        final var type = "rg";

        final var actual = assertThrows(DomainException.class, () -> Document.create(VALID_CPF, type));

        assertEquals("'rg' is not a valid document type", actual.getMessage());
    }

    // Na referência o switch sobre tipo nulo estoura NullPointerException, que viraria 500.
    @Test
    void givenANullType_whenCreatesFromTheFactory_thenThrowsDomainExceptionAndNotNullPointer() {
        final String type = null;

        final var actual = assertThrows(DomainException.class, () -> Document.create(VALID_CPF, type));

        assertEquals("'documentType' should not be empty", actual.getMessage());
    }
}
