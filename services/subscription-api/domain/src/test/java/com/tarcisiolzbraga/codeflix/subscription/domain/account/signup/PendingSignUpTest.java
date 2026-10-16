package com.tarcisiolzbraga.codeflix.subscription.domain.account.signup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tarcisiolzbraga.codeflix.subscription.domain.account.AccountFixture;
import com.tarcisiolzbraga.codeflix.subscription.domain.account.AccountId;
import com.tarcisiolzbraga.codeflix.subscription.domain.exceptions.ConflictException;
import com.tarcisiolzbraga.codeflix.subscription.domain.exceptions.DomainException;
import com.tarcisiolzbraga.codeflix.subscription.domain.utils.InstantUtils;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;

class PendingSignUpTest {

    @Test
    void givenValidData_whenCreates_thenHoldsWhatTheSignUpSaid() {
        final var actual = PendingSignUpFixture.pendingSignUp();

        assertEquals(AccountFixture.email(), actual.email());
        assertEquals(AccountFixture.name(), actual.name());
        assertEquals(AccountFixture.document(), actual.document());
        assertEquals(AccountFixture.userId(), actual.userId());
    }

    // O agregado nunca guarda o token em claro: ele fica com o resumo, e o claro vai no e-mail.
    @Test
    void givenAToken_whenCreates_thenItKeepsOnlyTheDigest() {
        final var token = SignUpToken.generate();

        final var actual = PendingSignUpFixture.pendingSignUpWith(token);

        assertEquals(token.hashed(), actual.tokenHash());
        assertNotEquals(token.value(), actual.tokenHash());
    }

    @Test
    void givenANewPendingSignUp_whenCreated_thenItIsNotConfirmedYet() {
        final var actual = PendingSignUpFixture.pendingSignUp();

        assertFalse(actual.isConfirmed());
        assertNull(actual.confirmedAt());
    }

    @Test
    void givenANewPendingSignUp_whenCreated_thenItStartsAtVersionZero() {
        final var actual = PendingSignUpFixture.pendingSignUp();

        assertEquals(0, actual.version());
    }

    @Test
    void givenAPendingSignUpWithinItsDeadline_whenConfirms_thenItBecomesConfirmed() {
        final var pending = PendingSignUpFixture.pendingSignUp();
        final var now = InstantUtils.now();

        pending.confirm(now);

        assertTrue(pending.isConfirmed());
        assertEquals(now, pending.confirmedAt());
    }

    // O prazo é do negócio, não da infraestrutura: quem decide se venceu é o agregado, com a hora
    // que o caso de uso lhe entrega — e não um relógio que ele mesmo consulta, o que o tornaria
    // impossível de testar sem esperar.
    @Test
    void givenAPendingSignUpPastItsDeadline_whenConfirms_thenRefuses() {
        final var expired = PendingSignUpFixture.storedPendingSignUp(
                SignUpToken.generate(), InstantUtils.now().minus(1, ChronoUnit.SECONDS));
        final var now = InstantUtils.now();

        final var actual = assertThrows(DomainException.class, () -> expired.confirm(now));

        assertEquals("the sign-up link expired; sign up again", actual.getMessage());
    }

    @Test
    void givenAPendingSignUpPastItsDeadline_whenConfirms_thenItStaysUnconfirmed() {
        final var expired = PendingSignUpFixture.storedPendingSignUp(
                SignUpToken.generate(), InstantUtils.now().minus(1, ChronoUnit.SECONDS));

        assertThrows(DomainException.class, () -> expired.confirm(InstantUtils.now()));

        assertFalse(expired.isConfirmed());
    }

    // Clicar no link duas vezes não cria duas contas. É conflito, não erro de validação: o pedido
    // estava certo, o estado é que não permite mais.
    @Test
    void givenAnAlreadyConfirmedPendingSignUp_whenConfirmsAgain_thenRefusesWithConflict() {
        final var pending = PendingSignUpFixture.pendingSignUp();
        pending.confirm(InstantUtils.now());

        final var actual = assertThrows(ConflictException.class, () -> pending.confirm(InstantUtils.now()));

        assertEquals("this sign-up was already confirmed", actual.getMessage());
    }

    @Test
    void givenAnAlreadyConfirmedPendingSignUp_whenConfirmsAgain_thenTheFirstMomentIsKept() {
        final var pending = PendingSignUpFixture.pendingSignUp();
        final var first = InstantUtils.now();
        pending.confirm(first);

        assertThrows(ConflictException.class, () -> pending.confirm(InstantUtils.now().plusSeconds(10)));

        assertEquals(first, pending.confirmedAt());
    }

    @Test
    void givenNullExpiry_whenCreates_thenThrowsDomainException() {
        final var actual = assertThrows(
                DomainException.class,
                () -> PendingSignUp.newPendingSignUp(
                        PendingSignUpId.unique(),
                        AccountFixture.userId(),
                        AccountFixture.email(),
                        AccountFixture.name(),
                        AccountFixture.document(),
                        SignUpToken.generate(),
                        null));

        assertEquals("'expiresAt' should not be null", actual.getMessage());
    }

    @Test
    void givenAPendingSignUp_whenAsksForTheAccountItWouldCreate_thenItCarriesEverythingNeeded() {
        final var pending = PendingSignUpFixture.pendingSignUp();

        final var actual = pending.toAccount(AccountId.unique());

        assertEquals(AccountFixture.email(), actual.email());
        assertEquals(AccountFixture.userId(), actual.userId());
        assertEquals(1, actual.domainEvents().size());
    }
}
