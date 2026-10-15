package com.tarcisiolzbraga.codeflix.subscription.application.account.signup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tarcisiolzbraga.codeflix.subscription.domain.account.Account;
import com.tarcisiolzbraga.codeflix.subscription.domain.account.AccountFixture;
import com.tarcisiolzbraga.codeflix.subscription.domain.account.AccountGateway;
import com.tarcisiolzbraga.codeflix.subscription.domain.account.AccountId;
import com.tarcisiolzbraga.codeflix.subscription.domain.account.idp.IdentityProviderGateway;
import com.tarcisiolzbraga.codeflix.subscription.domain.account.idp.UserId;
import com.tarcisiolzbraga.codeflix.subscription.domain.account.signup.PendingSignUp;
import com.tarcisiolzbraga.codeflix.subscription.domain.account.signup.PendingSignUpFixture;
import com.tarcisiolzbraga.codeflix.subscription.domain.account.signup.PendingSignUpGateway;
import com.tarcisiolzbraga.codeflix.subscription.domain.account.signup.SignUpToken;
import com.tarcisiolzbraga.codeflix.subscription.domain.exceptions.ConflictException;
import com.tarcisiolzbraga.codeflix.subscription.domain.exceptions.DomainException;
import com.tarcisiolzbraga.codeflix.subscription.domain.exceptions.NotFoundException;
import com.tarcisiolzbraga.codeflix.subscription.domain.person.Email;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ConfirmSignUpUseCaseTest {

    private static final Instant NOW = Instant.parse("2026-10-14T12:00:00Z");

    @Mock
    private AccountGateway accountGateway;

    @Mock
    private PendingSignUpGateway pendingSignUpGateway;

    @Mock
    private IdentityProviderGateway identityProviderGateway;

    @Captor
    private ArgumentCaptor<Account> savedAccount;

    @Captor
    private ArgumentCaptor<PendingSignUp> savedPending;

    private DefaultConfirmSignUpUseCase useCase() {
        return new DefaultConfirmSignUpUseCase(
                new ConfirmSignUpGateways(accountGateway, pendingSignUpGateway, identityProviderGateway),
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private static PendingSignUp pendingFor(final SignUpToken token) {
        return PendingSignUpFixture.storedPendingSignUp(token, NOW.plusSeconds(3600));
    }

    @Test
    void givenAValidToken_whenConfirms_thenCreatesTheAccountWithWhatWasSignedUp() {
        final var token = SignUpToken.generate();
        when(pendingSignUpGateway.pendingSignUpOfToken(any())).thenReturn(Optional.of(pendingFor(token)));
        when(accountGateway.accountOfEmail(any())).thenReturn(Optional.empty());
        when(accountGateway.save(any())).thenAnswer(returnsFirstArg());
        when(pendingSignUpGateway.save(any())).thenAnswer(returnsFirstArg());

        final var actual = useCase().execute(new ConfirmSignUpCommand(token.value()));

        verify(accountGateway).save(savedAccount.capture());
        assertEquals(AccountFixture.email(), savedAccount.getValue().email());
        assertEquals(AccountFixture.document(), savedAccount.getValue().document());
        assertEquals(savedAccount.getValue().id().getValue(), actual.accountId());
    }

    @Test
    void givenAValidToken_whenConfirms_thenMarksTheEmailVerifiedInTheProvider() {
        final var token = SignUpToken.generate();
        when(pendingSignUpGateway.pendingSignUpOfToken(any())).thenReturn(Optional.of(pendingFor(token)));
        when(accountGateway.accountOfEmail(any())).thenReturn(Optional.empty());
        when(accountGateway.save(any())).thenAnswer(returnsFirstArg());
        when(pendingSignUpGateway.save(any())).thenAnswer(returnsFirstArg());

        useCase().execute(new ConfirmSignUpCommand(token.value()));

        verify(identityProviderGateway).markEmailVerified(AccountFixture.userId());
    }

    // O que a confirmação concede é **conta**, não catálogo. A primeira versão disto dava a role de
    // assinante aqui, e aí quem só confirmava um e-mail passava a poder assistir sem pagar.
    @Test
    void givenAValidToken_whenConfirms_thenTheProviderGrantsAccountAccess() {
        final var token = SignUpToken.generate();
        when(pendingSignUpGateway.pendingSignUpOfToken(any())).thenReturn(Optional.of(pendingFor(token)));
        when(accountGateway.accountOfEmail(any())).thenReturn(Optional.empty());
        when(accountGateway.save(any())).thenAnswer(returnsFirstArg());
        when(pendingSignUpGateway.save(any())).thenAnswer(returnsFirstArg());

        useCase().execute(new ConfirmSignUpCommand(token.value()));

        verify(identityProviderGateway).grantAccountAccess(AccountFixture.userId());
    }

    // O teste que guarda o furo: confirmar um cadastro **não** dá acesso ao catálogo. A role de
    // assinante é o que a videos-api exige nas queries, e concedê-la aqui deixava qualquer pessoa
    // que confirmasse um e-mail assistir sem assinar e sem pagar. Quem a concede é a assinatura.
    @Test
    void givenAValidToken_whenConfirms_thenNoCatalogAccessIsGranted() {
        final var token = SignUpToken.generate();
        when(pendingSignUpGateway.pendingSignUpOfToken(any())).thenReturn(Optional.of(pendingFor(token)));
        when(accountGateway.accountOfEmail(any())).thenReturn(Optional.empty());
        when(accountGateway.save(any())).thenAnswer(returnsFirstArg());
        when(pendingSignUpGateway.save(any())).thenAnswer(returnsFirstArg());

        useCase().execute(new ConfirmSignUpCommand(token.value()));

        verify(identityProviderGateway, never()).grantCatalogAccess(any());
    }

    // O acesso é concedido depois de a conta existir: conceder antes deixaria, numa interrupção,
    // alguém com a role e sem conta neste serviço.
    @Test
    void givenAValidToken_whenConfirms_thenTheAccountIsStoredBeforeTheProviderIsTold() {
        final var token = SignUpToken.generate();
        when(pendingSignUpGateway.pendingSignUpOfToken(any())).thenReturn(Optional.of(pendingFor(token)));
        when(accountGateway.accountOfEmail(any())).thenReturn(Optional.empty());
        when(accountGateway.save(any())).thenAnswer(returnsFirstArg());
        when(pendingSignUpGateway.save(any())).thenAnswer(returnsFirstArg());

        useCase().execute(new ConfirmSignUpCommand(token.value()));

        final var order = inOrder(accountGateway, identityProviderGateway);
        order.verify(accountGateway).save(any());
        order.verify(identityProviderGateway).grantAccountAccess(AccountFixture.userId());
    }

    // O estado que uma interrupção entre gravar a conta e falar com o provedor deixa: conta criada
    // e nenhum acesso. Repetir conserta, e é por isso que a concessão está nos dois caminhos.
    @Test
    void givenAnInterruptionBeforeTheProviderWasTold_whenConfirmsAgain_thenGrantsTheAccess() {
        final var token = SignUpToken.generate();
        when(pendingSignUpGateway.pendingSignUpOfToken(any())).thenReturn(Optional.of(pendingFor(token)));
        when(accountGateway.accountOfEmail(any())).thenReturn(Optional.of(AccountFixture.account()));
        when(pendingSignUpGateway.save(any())).thenAnswer(returnsFirstArg());

        useCase().execute(new ConfirmSignUpCommand(token.value()));

        verify(accountGateway, never()).save(any());
        verify(identityProviderGateway).grantAccountAccess(AccountFixture.userId());
    }

    @Test
    void givenAValidToken_whenConfirms_thenThePendingIsStoredAsConfirmed() {
        final var token = SignUpToken.generate();
        when(pendingSignUpGateway.pendingSignUpOfToken(any())).thenReturn(Optional.of(pendingFor(token)));
        when(accountGateway.accountOfEmail(any())).thenReturn(Optional.empty());
        when(accountGateway.save(any())).thenAnswer(returnsFirstArg());
        when(pendingSignUpGateway.save(any())).thenAnswer(returnsFirstArg());

        useCase().execute(new ConfirmSignUpCommand(token.value()));

        verify(pendingSignUpGateway).save(savedPending.capture());
        assertTrue(savedPending.getValue().isConfirmed());
        assertEquals(NOW, savedPending.getValue().confirmedAt());
    }

    // A conta é gravada ANTES do pendente, de propósito. Se o processo morrer entre as duas, sobra
    // conta sem pendente confirmado — e a repetição conserta, porque ela reconhece esse estado. Na
    // ordem inversa sobraria pendente confirmado sem conta, e aí a pessoa ficaria travada.
    @Test
    void givenAValidToken_whenConfirms_thenTheAccountIsStoredBeforeThePending() {
        final var token = SignUpToken.generate();
        when(pendingSignUpGateway.pendingSignUpOfToken(any())).thenReturn(Optional.of(pendingFor(token)));
        when(accountGateway.accountOfEmail(any())).thenReturn(Optional.empty());
        when(accountGateway.save(any())).thenAnswer(returnsFirstArg());
        when(pendingSignUpGateway.save(any())).thenAnswer(returnsFirstArg());

        useCase().execute(new ConfirmSignUpCommand(token.value()));

        final var order = inOrder(accountGateway, pendingSignUpGateway);
        order.verify(accountGateway).save(any());
        order.verify(pendingSignUpGateway).save(any());
    }

    // O estado que a interrupção deixa: conta criada, pendente ainda não confirmado. Repetir
    // termina o serviço em vez de recusar.
    @Test
    void givenAnAccountAlreadyCreatedByAnInterruptedConfirmation_whenConfirmsAgain_thenItFinishesTheJob() {
        final var token = SignUpToken.generate();
        final var pending = pendingFor(token);
        final var account = AccountFixture.account();
        when(pendingSignUpGateway.pendingSignUpOfToken(any())).thenReturn(Optional.of(pending));
        when(accountGateway.accountOfEmail(any())).thenReturn(Optional.of(account));
        when(pendingSignUpGateway.save(any())).thenAnswer(returnsFirstArg());

        final var actual = useCase().execute(new ConfirmSignUpCommand(token.value()));

        assertEquals(account.id().getValue(), actual.accountId());
        verify(accountGateway, never()).save(any());
        verify(pendingSignUpGateway).save(savedPending.capture());
        assertTrue(savedPending.getValue().isConfirmed());
    }

    // Clicar no link duas vezes devolve a mesma conta, e não um erro.
    @Test
    void givenAnAlreadyConfirmedSignUp_whenConfirmsAgain_thenAnswersTheSameAccount() {
        final var token = SignUpToken.generate();
        final var pending = pendingFor(token);
        pending.confirm(NOW);
        final var account = AccountFixture.account();
        when(pendingSignUpGateway.pendingSignUpOfToken(any())).thenReturn(Optional.of(pending));
        when(accountGateway.accountOfEmail(any())).thenReturn(Optional.of(account));

        final var actual = useCase().execute(new ConfirmSignUpCommand(token.value()));

        assertEquals(account.id().getValue(), actual.accountId());
        verify(accountGateway, never()).save(any());
        verify(pendingSignUpGateway, never()).save(any());
    }

    // Conta de OUTRA pessoa com o mesmo e-mail é conflito de verdade: não se confirma por cima dela.
    @Test
    void givenTheEmailBelongsToAnotherProviderUser_whenConfirms_thenRefusesWithConflict() {
        final var token = SignUpToken.generate();
        when(pendingSignUpGateway.pendingSignUpOfToken(any())).thenReturn(Optional.of(pendingFor(token)));
        when(accountGateway.accountOfEmail(any())).thenReturn(Optional.of(accountOfSomeoneElse()));

        final var actual = assertThrows(
                ConflictException.class, () -> useCase().execute(new ConfirmSignUpCommand(token.value())));

        assertTrue(actual.getMessage().contains("already taken"));
        verify(accountGateway, never()).save(any());
    }

    @Test
    void givenAnUnknownToken_whenConfirms_thenAnswersNotFound() {
        when(pendingSignUpGateway.pendingSignUpOfToken(any())).thenReturn(Optional.empty());

        final var actual = assertThrows(
                NotFoundException.class,
                () -> useCase().execute(new ConfirmSignUpCommand(SignUpToken.generate().value())));

        assertTrue(actual.getMessage().contains("was not found"));
    }

    @Test
    void givenAnExpiredToken_whenConfirms_thenRefusesAndCreatesNothing() {
        final var token = SignUpToken.generate();
        final var expired = PendingSignUpFixture.storedPendingSignUp(token, NOW.minusSeconds(1));
        when(pendingSignUpGateway.pendingSignUpOfToken(any())).thenReturn(Optional.of(expired));
        when(accountGateway.accountOfEmail(any())).thenReturn(Optional.empty());

        final var actual = assertThrows(
                DomainException.class, () -> useCase().execute(new ConfirmSignUpCommand(token.value())));

        assertEquals("the sign-up link expired; sign up again", actual.getMessage());
        verify(accountGateway, never()).save(any());
    }

    @Test
    void givenABlankToken_whenConfirms_thenRefusesBeforeTouchingAnything() {
        final var actual =
                assertThrows(DomainException.class, () -> useCase().execute(new ConfirmSignUpCommand("  ")));

        assertEquals("'token' should not be empty", actual.getMessage());
        verify(pendingSignUpGateway, never()).pendingSignUpOfToken(any());
    }

    private static Account accountOfSomeoneElse() {
        return Account.with(
                AccountId.unique(),
                1,
                new UserId("outro-id-do-provedor"),
                new Email(AccountFixture.EMAIL),
                AccountFixture.name(),
                AccountFixture.document(),
                null);
    }
}
