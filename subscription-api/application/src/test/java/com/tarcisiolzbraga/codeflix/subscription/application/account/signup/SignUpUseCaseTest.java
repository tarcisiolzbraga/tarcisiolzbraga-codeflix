package com.tarcisiolzbraga.codeflix.subscription.application.account.signup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tarcisiolzbraga.codeflix.subscription.domain.account.AccountFixture;
import com.tarcisiolzbraga.codeflix.subscription.domain.account.AccountGateway;
import com.tarcisiolzbraga.codeflix.subscription.domain.account.idp.IdentityProviderGateway;
import com.tarcisiolzbraga.codeflix.subscription.domain.account.idp.User;
import com.tarcisiolzbraga.codeflix.subscription.domain.account.signup.PendingSignUp;
import com.tarcisiolzbraga.codeflix.subscription.domain.account.signup.PendingSignUpGateway;
import com.tarcisiolzbraga.codeflix.subscription.domain.account.signup.SignUpNotifier;
import com.tarcisiolzbraga.codeflix.subscription.domain.account.signup.SignUpValidation;
import com.tarcisiolzbraga.codeflix.subscription.domain.exceptions.DomainException;
import java.time.Clock;
import java.time.Duration;
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
class SignUpUseCaseTest {

    private static final Instant NOW = Instant.parse("2026-10-14T12:00:00Z");
    private static final Duration TTL = Duration.ofHours(24);
    private static final String PASSWORD = "uma-senha-boa";

    @Mock
    private AccountGateway accountGateway;

    @Mock
    private PendingSignUpGateway pendingSignUpGateway;

    @Mock
    private IdentityProviderGateway identityProviderGateway;

    @Mock
    private SignUpNotifier notifier;

    @Captor
    private ArgumentCaptor<PendingSignUp> savedPending;

    @Captor
    private ArgumentCaptor<SignUpValidation> sentValidation;

    private DefaultSignUpUseCase useCase() {
        return new DefaultSignUpUseCase(
                new SignUpGateways(accountGateway, pendingSignUpGateway, identityProviderGateway, notifier),
                Clock.fixed(NOW, ZoneOffset.UTC),
                TTL);
    }

    private static SignUpCommand command() {
        return SignUpCommand.with(
                AccountFixture.FIRSTNAME,
                AccountFixture.LASTNAME,
                AccountFixture.EMAIL,
                PASSWORD,
                AccountFixture.CPF,
                "cpf");
    }

    @Test
    void givenANewEmail_whenSignsUp_thenCreatesTheUserInTheProvider() {
        when(accountGateway.accountOfEmail(any())).thenReturn(Optional.empty());
        when(identityProviderGateway.userOfEmail(any())).thenReturn(Optional.empty());
        when(identityProviderGateway.create(any())).thenReturn(AccountFixture.userId());
        when(pendingSignUpGateway.save(any())).thenAnswer(returnsFirstArg());

        useCase().execute(command());

        verify(identityProviderGateway).create(any());
    }

    @Test
    void givenANewEmail_whenSignsUp_thenStoresThePendingWithWhatWasSent() {
        when(accountGateway.accountOfEmail(any())).thenReturn(Optional.empty());
        when(identityProviderGateway.userOfEmail(any())).thenReturn(Optional.empty());
        when(identityProviderGateway.create(any())).thenReturn(AccountFixture.userId());
        when(pendingSignUpGateway.save(any())).thenAnswer(returnsFirstArg());

        useCase().execute(command());

        verify(pendingSignUpGateway).save(savedPending.capture());
        assertEquals(AccountFixture.email(), savedPending.getValue().email());
        assertEquals(AccountFixture.document(), savedPending.getValue().document());
    }

    @Test
    void givenANewEmail_whenSignsUp_thenThePendingExpiresAfterTheConfiguredTime() {
        when(accountGateway.accountOfEmail(any())).thenReturn(Optional.empty());
        when(identityProviderGateway.userOfEmail(any())).thenReturn(Optional.empty());
        when(identityProviderGateway.create(any())).thenReturn(AccountFixture.userId());
        when(pendingSignUpGateway.save(any())).thenAnswer(returnsFirstArg());

        useCase().execute(command());

        verify(pendingSignUpGateway).save(savedPending.capture());
        assertEquals(NOW.plus(TTL), savedPending.getValue().expiresAt());
    }

    // O e-mail já existir no provedor não é erro: é o caso que a validação existe para cobrir. Não
    // se cria usuário nem se toca na senha dele — ninguém sobrescreve a senha de outra pessoa.
    @Test
    void givenAnEmailThatAlreadyExistsInTheProvider_whenSignsUp_thenReusesItWithoutTouchingThePassword() {
        when(accountGateway.accountOfEmail(any())).thenReturn(Optional.empty());
        when(identityProviderGateway.userOfEmail(any())).thenReturn(Optional.of(existingUser()));
        when(pendingSignUpGateway.save(any())).thenAnswer(returnsFirstArg());

        useCase().execute(command());

        verify(identityProviderGateway, never()).create(any());
        verify(pendingSignUpGateway).save(savedPending.capture());
        assertEquals(AccountFixture.userId(), savedPending.getValue().userId());
    }

    // Já existir conta é o fim da linha: não há o que confirmar. A resposta é a mesma, e é por isso
    // que o caso de uso não devolve nada que distinga os casos.
    @Test
    void givenAnEmailThatAlreadyHasAnAccount_whenSignsUp_thenNothingIsStoredAndNothingIsSent() {
        when(accountGateway.accountOfEmail(any())).thenReturn(Optional.of(AccountFixture.account()));

        useCase().execute(command());

        verify(pendingSignUpGateway, never()).save(any());
        verify(notifier, never()).sendValidation(any());
        verify(identityProviderGateway, never()).create(any());
    }

    // Pedir de novo é legítimo: a mensagem pode não ter chegado. Só o link mais novo vale.
    @Test
    void givenAPreviousPendingOfTheSameEmail_whenSignsUpAgain_thenTheOlderOnesAreRemovedFirst() {
        when(accountGateway.accountOfEmail(any())).thenReturn(Optional.empty());
        when(identityProviderGateway.userOfEmail(any())).thenReturn(Optional.of(existingUser()));
        when(pendingSignUpGateway.save(any())).thenAnswer(returnsFirstArg());

        useCase().execute(command());

        final var order = inOrder(pendingSignUpGateway);
        order.verify(pendingSignUpGateway).deleteUnconfirmedOf(AccountFixture.email());
        order.verify(pendingSignUpGateway).save(any());
    }

    @Test
    void givenANewEmail_whenSignsUp_thenTheValidationCarriesTheTokenInTheClear() {
        when(accountGateway.accountOfEmail(any())).thenReturn(Optional.empty());
        when(identityProviderGateway.userOfEmail(any())).thenReturn(Optional.empty());
        when(identityProviderGateway.create(any())).thenReturn(AccountFixture.userId());
        when(pendingSignUpGateway.save(any())).thenAnswer(returnsFirstArg());

        useCase().execute(command());

        verify(notifier).sendValidation(sentValidation.capture());
        verify(pendingSignUpGateway).save(savedPending.capture());
        assertEquals(savedPending.getValue().tokenHash(), sentValidation.getValue().token().hashed());
    }

    // Grava e só então publica: falhar ao publicar significa que nenhuma mensagem saiu, e pedir o
    // cadastro de novo substitui o pendente e manda outra. A ordem inversa mandaria link para um
    // cadastro que não existe.
    @Test
    void givenANewEmail_whenSignsUp_thenItStoresBeforeSending() {
        when(accountGateway.accountOfEmail(any())).thenReturn(Optional.empty());
        when(identityProviderGateway.userOfEmail(any())).thenReturn(Optional.empty());
        when(identityProviderGateway.create(any())).thenReturn(AccountFixture.userId());
        when(pendingSignUpGateway.save(any())).thenAnswer(returnsFirstArg());

        useCase().execute(command());

        final var order = inOrder(pendingSignUpGateway, notifier);
        order.verify(pendingSignUpGateway).save(any());
        order.verify(notifier).sendValidation(any());
    }

    @Test
    void givenAnInvalidDocument_whenSignsUp_thenThrowsAndTouchesNothing() {
        final var command = SignUpCommand.with(
                AccountFixture.FIRSTNAME, AccountFixture.LASTNAME, AccountFixture.EMAIL, PASSWORD, "11111111111", "cpf");

        final var actual = assertThrows(DomainException.class, () -> useCase().execute(command));

        assertEquals("'cpf' is invalid", actual.getMessage());
        verify(identityProviderGateway, never()).create(any());
        verify(pendingSignUpGateway, never()).save(any());
    }

    @Test
    void givenAPasswordTooShort_whenSignsUp_thenThrowsAndTouchesNothing() {
        final var command = SignUpCommand.with(
                AccountFixture.FIRSTNAME,
                AccountFixture.LASTNAME,
                AccountFixture.EMAIL,
                "curta",
                AccountFixture.CPF,
                "cpf");

        final var actual = assertThrows(DomainException.class, () -> useCase().execute(command));

        assertEquals("'password' should have at least 8 characters", actual.getMessage());
        verify(identityProviderGateway, never()).create(any());
    }

    private static User existingUser() {
        return new User(AccountFixture.userId(), AccountFixture.name(), AccountFixture.email(), true, false);
    }
}
