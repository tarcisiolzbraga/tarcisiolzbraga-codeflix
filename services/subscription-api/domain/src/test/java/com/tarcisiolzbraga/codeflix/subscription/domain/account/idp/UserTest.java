package com.tarcisiolzbraga.codeflix.subscription.domain.account.idp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tarcisiolzbraga.codeflix.subscription.domain.account.AccountFixture;
import com.tarcisiolzbraga.codeflix.subscription.domain.exceptions.DomainException;
import org.junit.jupiter.api.Test;

class UserTest {

    @Test
    void givenAUserFromTheProvider_whenCreates_thenHoldsWhatItSaid() {
        final var userId = AccountFixture.userId();

        final var actual = new User(userId, AccountFixture.name(), AccountFixture.email(), true, false);

        assertEquals(userId, actual.userId());
        assertTrue(actual.enabled());
        assertFalse(actual.emailVerified());
    }

    // Não carrega senha, ao contrário do User da referência: senha é dado de criação, e um usuário
    // lido do provedor nunca a traz de volta. Juntar as duas coisas num tipo só deixava um campo
    // sempre nulo na leitura e obrigatório na escrita.
    @Test
    void givenNullUserId_whenCreates_thenThrowsDomainException() {
        final UserId userId = null;

        final var actual = assertThrows(
                DomainException.class,
                () -> new User(userId, AccountFixture.name(), AccountFixture.email(), true, false));

        assertEquals("'userId' should not be null", actual.getMessage());
    }

    @Test
    void givenNullEmail_whenCreates_thenThrowsDomainException() {
        final var actual = assertThrows(
                DomainException.class,
                () -> new User(AccountFixture.userId(), AccountFixture.name(), null, true, false));

        assertEquals("'email' should not be null", actual.getMessage());
    }
}
