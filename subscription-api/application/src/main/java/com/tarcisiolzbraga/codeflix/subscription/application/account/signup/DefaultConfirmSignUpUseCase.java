package com.tarcisiolzbraga.codeflix.subscription.application.account.signup;

import com.tarcisiolzbraga.codeflix.subscription.domain.account.Account;
import com.tarcisiolzbraga.codeflix.subscription.domain.account.AccountId;
import com.tarcisiolzbraga.codeflix.subscription.domain.account.signup.PendingSignUp;
import com.tarcisiolzbraga.codeflix.subscription.domain.account.signup.SignUpToken;
import com.tarcisiolzbraga.codeflix.subscription.domain.exceptions.ConflictException;
import com.tarcisiolzbraga.codeflix.subscription.domain.exceptions.NotFoundException;
import java.time.Clock;
import java.util.Objects;

// A confirmação: é aqui que a conta nasce, depois de a pessoa provar que leu o e-mail.
//
// A ordem das gravações é escolhida para que interrupção no meio seja consertável por repetição, e
// não por intervenção. A conta vai primeiro: se o processo morrer antes de o pendente ser marcado,
// sobra conta sem confirmação, e repetir reconhece esse estado e termina o serviço. Na ordem
// inversa sobraria pendente confirmado sem conta, e a pessoa ficaria travada — o token já teria
// sido gasto e nenhuma conta existiria.
//
// Por isso não há transação envolvendo as duas escritas: ela também não resolveria a chamada ao
// provedor de identidade, que vem depois e não participa de transação de banco.
public class DefaultConfirmSignUpUseCase extends ConfirmSignUpUseCase {

    private final ConfirmSignUpGateways gateways;
    private final Clock clock;

    public DefaultConfirmSignUpUseCase(final ConfirmSignUpGateways gateways, final Clock clock) {
        this.gateways = Objects.requireNonNull(gateways, "'gateways' should not be null");
        this.clock = Objects.requireNonNull(clock, "'clock' should not be null");
    }

    @Override
    public ConfirmSignUpOutput execute(final ConfirmSignUpCommand command) {
        Objects.requireNonNull(command, "'command' should not be null");
        final var token = new SignUpToken(command.token());

        final var pending = this.gateways
                .pendingSignUps()
                .pendingSignUpOfToken(token)
                .orElseThrow(NotFoundException::signUpLink);

        final var existing = this.gateways.accounts().accountOfEmail(pending.email());
        if (existing.isPresent()) {
            return finishOrRefuse(pending, existing.get());
        }

        final var now = this.clock.instant();
        pending.confirm(now);

        final var account = this.gateways.accounts().save(pending.toAccount(AccountId.unique()));
        this.gateways.pendingSignUps().save(pending);
        grantSubscriberAccess(pending);

        return ConfirmSignUpOutput.from(account);
    }

    // Já existe conta para este e-mail. São dois casos muito diferentes:
    //
    // - é a mesma pessoa do provedor: ou a confirmação foi interrompida depois de criar a conta, ou
    //   a pessoa clicou no link duas vezes. Nos dois, terminar o que falta e devolver a conta é mais
    //   útil que recusar.
    // - é outra pessoa do provedor: o e-mail pertence a outra conta, e confirmar por cima dela seria
    //   entregar a conta de alguém a quem tem o token.
    private ConfirmSignUpOutput finishOrRefuse(final PendingSignUp pending, final Account existing) {
        if (!existing.userId().equals(pending.userId())) {
            throw ConflictException.emailAlreadyTaken(pending.email().value());
        }
        if (!pending.isConfirmed()) {
            pending.confirm(this.clock.instant());
            this.gateways.pendingSignUps().save(pending);
        }
        // Repetido aqui, e não só no caminho de cima: é o que faz repetir consertar: se o processo
        // morreu depois de gravar a conta e antes de falar com o provedor, a pessoa teria conta e
        // nenhum acesso, e clicar no link de novo termina o serviço.
        grantSubscriberAccess(pending);
        return ConfirmSignUpOutput.from(existing);
    }

    // A role de assinante vem junto com a conta, e não com a assinatura.
    //
    // É tentador amarrá-la a ter assinatura viva, mas aí nasceria um nó: as rotas de assinatura
    // exigem a role, e sem a role não haveria como assinar. O que a role diz é "tem conta neste
    // serviço"; quem tem direito ao catálogo é outra pergunta, decidida pelo estado da assinatura e
    // de quem serve o vídeo, não por grupo no provedor de identidade.
    //
    // As duas chamadas são idempotentes do lado do Keycloak: conferir e-mail já conferido e pôr num
    // grupo em que já está não mudam nada, e é isso que permite chamá-las em dois caminhos.
    private void grantSubscriberAccess(final PendingSignUp pending) {
        this.gateways.identityProvider().markEmailVerified(pending.userId());
        this.gateways.identityProvider().addToSubscribers(pending.userId());
    }
}
