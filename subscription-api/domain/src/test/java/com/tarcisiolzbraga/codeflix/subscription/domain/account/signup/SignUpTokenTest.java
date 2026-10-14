package com.tarcisiolzbraga.codeflix.subscription.domain.account.signup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tarcisiolzbraga.codeflix.subscription.domain.exceptions.DomainException;
import org.junit.jupiter.api.Test;

// O token é a credencial que vai no e-mail: quem o tem confirma o cadastro. Por isso ele é
// sorteado, e por isso o banco guarda só o resumo dele.
class SignUpTokenTest {

    @Test
    void givenNothing_whenGenerates_thenEachCallGivesADifferentToken() {
        final var one = SignUpToken.generate();

        final var actual = SignUpToken.generate();

        assertNotEquals(one, actual);
    }

    @Test
    void givenAGeneratedToken_whenAsksItsValue_thenItIsLongEnoughToResistGuessing() {
        final var actual = SignUpToken.generate();

        final var value = actual.value();

        assertTrue(value.length() >= 32, "o token veio com " + value.length() + " caracteres");
    }

    @Test
    void givenAGeneratedToken_whenAsksItsValue_thenItIsSafeInAUrl() {
        final var actual = SignUpToken.generate().value();

        final var safe = actual.matches("[A-Za-z0-9_-]+");

        assertTrue(safe, "o token precisa caber numa URL sem escape: " + actual);
    }

    // O resumo é o que a tabela guarda: um vazamento do banco não entrega tokens utilizáveis.
    @Test
    void givenTheSameToken_whenHashedTwice_thenTheDigestIsTheSame() {
        final var token = SignUpToken.generate();

        final var actual = token.hashed();

        assertEquals(token.hashed(), actual);
    }

    @Test
    void givenDifferentTokens_whenHashed_thenTheDigestsDiffer() {
        final var one = SignUpToken.generate();

        final var actual = SignUpToken.generate().hashed();

        assertNotEquals(one.hashed(), actual);
    }

    @Test
    void givenAToken_whenHashed_thenTheDigestIsNotTheTokenItself() {
        final var token = SignUpToken.generate();

        final var actual = token.hashed();

        assertNotEquals(token.value(), actual);
    }

    @Test
    void givenTextFromTheOutside_whenCreatesFromIt_thenItHashesTheSameAsTheOriginal() {
        final var original = SignUpToken.generate();

        final var actual = new SignUpToken(original.value());

        assertEquals(original.hashed(), actual.hashed());
    }

    @Test
    void givenBlank_whenCreates_thenThrowsDomainException() {
        final var value = "   ";

        final var actual = assertThrows(DomainException.class, () -> new SignUpToken(value));

        assertEquals("'token' should not be empty", actual.getMessage());
    }
}
