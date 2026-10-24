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
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;

@IntegrationTest
class PendingSignUpJdbcGatewayIT {

    // O mesmo padrão do `signup.retention`: o teste fixa a janela em vez de a ler da configuração,
    // para medir o comportamento do DELETE e não o valor escolhido.
    private static final Duration RETENTION = Duration.ofDays(7);

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

    // --- Retenção de dado pessoal ---------------------------------------------------------------
    //
    // Os quatro casos são os dois lados do limiar, nas duas condições. É o que prova que a janela
    // existe: apagar cedo demais troca o 422 de "o link venceu" por um 404, e apagar nunca é o
    // defeito que motivou isto.

    @Test
    void givenAnUnconfirmedSignUpExpiredLongAgo_whenRemovesExpired_thenItIsGone() {
        final var token = SignUpToken.generate();
        gateway.save(PendingSignUpFixture.pendingSignUpWith(token));
        expiresAt(token, InstantUtils.now().minus(Duration.ofDays(30)));

        final var removed = gateway.deleteExpiredBefore(InstantUtils.now().minus(RETENTION));

        assertEquals(1, removed);
        assertTrue(gateway.pendingSignUpOfToken(token).isEmpty());
    }

    // Vencido ontem **sobrevive**, e isso é o ponto: é ele que faz a API responder 422 "o link
    // venceu" em vez de 404, para quem clica num link de ontem.
    @Test
    void givenAnUnconfirmedSignUpExpiredYesterday_whenRemovesExpired_thenItSurvives() {
        final var token = SignUpToken.generate();
        gateway.save(PendingSignUpFixture.pendingSignUpWith(token));
        expiresAt(token, InstantUtils.now().minus(Duration.ofDays(1)));

        final var removed = gateway.deleteExpiredBefore(InstantUtils.now().minus(RETENTION));

        assertEquals(0, removed);
        assertTrue(gateway.pendingSignUpOfToken(token).isPresent());
    }

    // Confirmado há muito tempo sai: a conta já existe, e o link daquela idade não vale mais ser
    // repetido. O que fica guardado é a conta, não esta cópia do nome e do documento.
    @Test
    void givenASignUpConfirmedLongAgo_whenRemovesExpired_thenItIsGone() {
        final var token = SignUpToken.generate();
        gateway.save(PendingSignUpFixture.pendingSignUpWith(token));
        confirmedAt(token, InstantUtils.now().minus(Duration.ofDays(30)));

        final var removed = gateway.deleteExpiredBefore(InstantUtils.now().minus(RETENTION));

        assertEquals(1, removed);
        assertTrue(gateway.pendingSignUpOfToken(token).isEmpty());
    }

    // Confirmado hoje sobrevive: é o clique repetido no mesmo link, que tem de devolver a conta em
    // vez de erro.
    @Test
    void givenASignUpConfirmedToday_whenRemovesExpired_thenItSurvives() {
        final var token = SignUpToken.generate();
        gateway.save(PendingSignUpFixture.pendingSignUpWith(token));
        confirmedAt(token, InstantUtils.now());

        final var removed = gateway.deleteExpiredBefore(InstantUtils.now().minus(RETENTION));

        assertEquals(0, removed);
        assertTrue(gateway.pendingSignUpOfToken(token).isPresent());
    }

    // SQL direto para pôr a linha na idade que o caso pede: o tempo é a entrada do teste, e passar
    // por um relógio de mentira aqui esconderia qual coluna o DELETE de fato olha.
    private void expiresAt(final SignUpToken token, final Instant moment) {
        touch("UPDATE pending_sign_up SET expires_at = :moment WHERE token_hash = :hash", token, moment);
    }

    private void confirmedAt(final SignUpToken token, final Instant moment) {
        touch("UPDATE pending_sign_up SET confirmed_at = :moment WHERE token_hash = :hash", token, moment);
    }

    private void touch(final String sql, final SignUpToken token, final Instant moment) {
        jdbcClient.sql(sql).param("moment", moment).param("hash", token.hashed()).update();
    }
}
