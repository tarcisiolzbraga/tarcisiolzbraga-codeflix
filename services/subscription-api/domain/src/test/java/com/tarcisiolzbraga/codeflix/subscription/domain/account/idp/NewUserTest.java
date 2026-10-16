package com.tarcisiolzbraga.codeflix.subscription.domain.account.idp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.tarcisiolzbraga.codeflix.subscription.domain.account.AccountFixture;
import com.tarcisiolzbraga.codeflix.subscription.domain.exceptions.DomainException;
import com.tarcisiolzbraga.codeflix.subscription.domain.person.Email;
import com.tarcisiolzbraga.codeflix.subscription.domain.person.Name;
import org.junit.jupiter.api.Test;

class NewUserTest {

    private static final String PASSWORD = "uma-senha-boa";

    @Test
    void givenValidData_whenCreates_thenHoldsIt() {
        final var actual = new NewUser(AccountFixture.name(), AccountFixture.email(), PASSWORD);

        assertEquals(AccountFixture.email(), actual.email());
        assertEquals(AccountFixture.name(), actual.name());
    }

    @Test
    void givenNullName_whenCreates_thenThrowsDomainException() {
        final Name name = null;

        final var actual =
                assertThrows(DomainException.class, () -> new NewUser(name, AccountFixture.email(), PASSWORD));

        assertEquals("'name' should not be null", actual.getMessage());
    }

    @Test
    void givenNullEmail_whenCreates_thenThrowsDomainException() {
        final Email email = null;

        final var actual =
                assertThrows(DomainException.class, () -> new NewUser(AccountFixture.name(), email, PASSWORD));

        assertEquals("'email' should not be null", actual.getMessage());
    }

    @Test
    void givenBlankPassword_whenCreates_thenThrowsDomainException() {
        final var password = "   ";

        final var actual = assertThrows(DomainException.class, () -> newUserWith(password));

        assertEquals("'password' should not be empty", actual.getMessage());
    }

    // Mínimo de verdade, ao contrário da referência, que só põe um teto de 28 caracteres.
    @Test
    void givenAPasswordShorterThanTheMinimum_whenCreates_thenThrowsDomainException() {
        final var password = "1234567";

        final var actual = assertThrows(DomainException.class, () -> newUserWith(password));

        assertEquals("'password' should have at least 8 characters", actual.getMessage());
    }

    @Test
    void givenAPasswordAtTheMinimum_whenCreates_thenItIsAccepted() {
        final var password = "12345678";

        final var actual = newUserWith(password);

        assertEquals(password, actual.password());
    }

    // 72 bytes é onde o bcrypt para de olhar: senha maior passaria uma confiança que não existe.
    @Test
    void givenAPasswordLongerThanBcryptLooksAt_whenCreates_thenThrowsDomainException() {
        final var password = "a".repeat(73);

        final var actual = assertThrows(DomainException.class, () -> newUserWith(password));

        assertEquals("'password' should not have more than 72 characters", actual.getMessage());
    }

    private static NewUser newUserWith(final String password) {
        return new NewUser(AccountFixture.name(), AccountFixture.email(), password);
    }
}
