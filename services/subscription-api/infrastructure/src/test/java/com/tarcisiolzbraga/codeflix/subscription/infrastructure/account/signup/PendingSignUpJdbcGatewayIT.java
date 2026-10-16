package com.tarcisiolzbraga.codeflix.subscription.infrastructure.account.signup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tarcisiolzbraga.codeflix.subscription.domain.account.AccountFixture;
import com.tarcisiolzbraga.codeflix.subscription.domain.account.signup.PendingSignUpFixture;
import com.tarcisiolzbraga.codeflix.subscription.domain.account.signup.SignUpToken;
import com.tarcisiolzbraga.codeflix.subscription.domain.exceptions.ConflictException;
import com.tarcisiolzbraga.codeflix.subscription.domain.person.Email;
import com.tarcisiolzbraga.codeflix.subscription.domain.utils.InstantUtils;
import com.tarcisiolzbraga.codeflix.subscription.infrastructure.IntegrationTest;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;

@IntegrationTest
class PendingSignUpJdbcGatewayIT {

    @Autowired
    private PendingSignUpJdbcGateway gateway;

    @Autowired
    private JdbcClient jdbcClient;

    @Test
    void givenAPendingSignUp_whenSavesIt_thenItIsFoundByItsToken() {
        final var token = SignUpToken.generate();
        gateway.save(PendingSignUpFixture.pendingSignUpWith(token));

        final var actual = gateway.pendingSignUpOfToken(token).orElseThrow();

        assertEquals(AccountFixture.email(), actual.email());
        assertEquals(AccountFixture.name(), actual.name());
        assertEquals(AccountFixture.document(), actual.document());
    }

    // O token em claro não está no banco: a coluna guarda o resumo, e é por ele que se procura.
    @Test
    void givenASavedPendingSignUp_whenLooksAtTheColumn_thenTheTokenItselfIsNotThere() {
        final var token = SignUpToken.generate();
        gateway.save(PendingSignUpFixture.pendingSignUpWith(token));

        final var stored = this.jdbcClient
                .sql("SELECT token_hash FROM pending_sign_up WHERE token_hash = :hash")
                .params(Map.<String, Object>of("hash", token.hashed()))
                .query(String.class)
                .single();

        assertEquals(token.hashed(), stored);
        assertFalse(stored.contains(token.value()));
    }

    @Test
    void givenAnotherToken_whenLooksItUp_thenNothingComesBack() {
        gateway.save(PendingSignUpFixture.pendingSignUpWith(SignUpToken.generate()));

        final var actual = gateway.pendingSignUpOfToken(SignUpToken.generate());

        assertTrue(actual.isEmpty());
    }

    @Test
    void givenANewPendingSignUp_whenSavesIt_thenItIsStoredUnconfirmedAtVersionOne() {
        final var token = SignUpToken.generate();
        gateway.save(PendingSignUpFixture.pendingSignUpWith(token));

        final var actual = gateway.pendingSignUpOfToken(token).orElseThrow();

        assertEquals(1, actual.version());
        assertFalse(actual.isConfirmed());
        assertNull(actual.confirmedAt());
    }

    @Test
    void givenAStoredPendingSignUp_whenConfirmedAndSaved_thenTheMomentIsStored() {
        final var token = SignUpToken.generate();
        gateway.save(PendingSignUpFixture.pendingSignUpWith(token));
        final var stored = gateway.pendingSignUpOfToken(token).orElseThrow();
        final var now = InstantUtils.now();
        stored.confirm(now);

        gateway.save(stored);

        final var actual = gateway.pendingSignUpOfToken(token).orElseThrow();
        assertTrue(actual.isConfirmed());
        assertEquals(now, actual.confirmedAt());
    }

    @Test
    void givenAStoredPendingSignUp_whenConfirmedAndSaved_thenTheRestOfTheRowIsUntouched() {
        final var token = SignUpToken.generate();
        gateway.save(PendingSignUpFixture.pendingSignUpWith(token));
        final var stored = gateway.pendingSignUpOfToken(token).orElseThrow();
        stored.confirm(InstantUtils.now());

        gateway.save(stored);

        final var actual = gateway.pendingSignUpOfToken(token).orElseThrow();
        assertEquals(AccountFixture.email(), actual.email());
        assertEquals(AccountFixture.document(), actual.document());
        assertEquals(stored.expiresAt(), actual.expiresAt());
    }

    @Test
    void givenAPendingSignUpAtAVersionThatIsNoLongerCurrent_whenSavesIt_thenRefusesWithConflict() {
        final var token = SignUpToken.generate();
        gateway.save(PendingSignUpFixture.pendingSignUpWith(token));
        final var stored = gateway.pendingSignUpOfToken(token).orElseThrow();
        stored.confirm(InstantUtils.now());
        gateway.save(stored);

        final var actual = assertThrows(ConflictException.class, () -> gateway.save(stored));

        assertTrue(actual.getMessage().contains("was changed by someone else"));
    }

    // Pedir o cadastro de novo é legítimo, e só o link mais novo pode valer.
    @Test
    void givenUnconfirmedPendingSignUpsOfAnEmail_whenDeletesThem_thenTheirTokensStopWorking() {
        final var old = SignUpToken.generate();
        gateway.save(PendingSignUpFixture.pendingSignUpWith(old));

        final var removed = gateway.deleteUnconfirmedOf(AccountFixture.email());

        assertEquals(1, removed);
        assertTrue(gateway.pendingSignUpOfToken(old).isEmpty());
    }

    // O que já foi confirmado é histórico, e apagar não é assunto do cadastro novo.
    @Test
    void givenAConfirmedPendingSignUp_whenDeletesTheUnconfirmedOnes_thenItSurvives() {
        final var token = SignUpToken.generate();
        gateway.save(PendingSignUpFixture.pendingSignUpWith(token));
        final var stored = gateway.pendingSignUpOfToken(token).orElseThrow();
        stored.confirm(InstantUtils.now());
        gateway.save(stored);

        final var removed = gateway.deleteUnconfirmedOf(AccountFixture.email());

        assertEquals(0, removed);
        assertTrue(gateway.pendingSignUpOfToken(token).isPresent());
    }

    @Test
    void givenAnotherEmail_whenDeletesItsUnconfirmedOnes_thenOursSurvives() {
        final var token = SignUpToken.generate();
        gateway.save(PendingSignUpFixture.pendingSignUpWith(token));

        final var removed = gateway.deleteUnconfirmedOf(new Email("outra@codeflix.com.br"));

        assertEquals(0, removed);
        assertTrue(gateway.pendingSignUpOfToken(token).isPresent());
    }

    @Test
    void givenNothingPending_whenDeletesByEmail_thenNothingIsRemoved() {
        final var actual = gateway.deleteUnconfirmedOf(AccountFixture.email());

        assertEquals(0, actual);
    }
}
