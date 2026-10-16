package com.tarcisiolzbraga.codeflix.subscription.application.account.signup;

import com.tarcisiolzbraga.codeflix.subscription.domain.account.idp.NewUser;
import com.tarcisiolzbraga.codeflix.subscription.domain.account.idp.UserId;
import com.tarcisiolzbraga.codeflix.subscription.domain.account.signup.PendingSignUp;
import com.tarcisiolzbraga.codeflix.subscription.domain.account.signup.PendingSignUpId;
import com.tarcisiolzbraga.codeflix.subscription.domain.account.signup.SignUpToken;
import com.tarcisiolzbraga.codeflix.subscription.domain.account.signup.SignUpValidation;
import com.tarcisiolzbraga.codeflix.subscription.domain.person.Document;
import com.tarcisiolzbraga.codeflix.subscription.domain.person.Email;
import com.tarcisiolzbraga.codeflix.subscription.domain.person.Name;
import java.time.Clock;
import java.time.Duration;
import java.util.Objects;

// O cadastro: cria a pessoa no provedor de identidade, guarda o pendente e manda validar o e-mail. A
// conta local não nasce aqui — ela nasce na confirmação.
public class DefaultSignUpUseCase extends SignUpUseCase {

    private final SignUpGateways gateways;
    private final Clock clock;
    private final Duration validationTtl;

    public DefaultSignUpUseCase(
            final SignUpGateways gateways, final Clock clock, final Duration validationTtl) {
        this.gateways = Objects.requireNonNull(gateways, "'gateways' should not be null");
        this.clock = Objects.requireNonNull(clock, "'clock' should not be null");
        this.validationTtl = Objects.requireNonNull(validationTtl, "'validationTtl' should not be null");
    }

    @Override
    public void execute(final SignUpCommand command) {
        Objects.requireNonNull(command, "'command' should not be null");

        // Os value objects recusam o que estiver inválido antes de qualquer efeito acontecer.
        final var email = new Email(command.email());
        final var name = new Name(command.firstname(), command.lastname());
        final var document = Document.create(command.documentNumber(), command.documentType());
        final var newUser = new NewUser(name, email, command.password());

        // Já existir conta é o fim da linha: não há o que confirmar. Sai em silêncio, e a resposta
        // continua sendo a genérica — não se revela que a conta existe.
        if (this.gateways.accounts().accountOfEmail(email).isPresent()) {
            return;
        }

        final var userId = userIdFor(email, newUser);
        final var token = SignUpToken.generate();

        // Só o link mais novo vale: pedir o cadastro de novo é legítimo, porque a mensagem pode não
        // ter chegado.
        this.gateways.pendingSignUps().deleteUnconfirmedOf(email);

        final var pending = PendingSignUp.newPendingSignUp(
                PendingSignUpId.unique(),
                userId,
                email,
                name,
                document,
                token,
                this.clock.instant().plus(this.validationTtl));

        // Grava e só então publica. Na ordem inversa, uma falha ao gravar mandaria link para um
        // cadastro que não existe; nesta, uma falha ao publicar deixa o pendente sem mensagem — e
        // pedir o cadastro de novo o substitui e manda outra.
        this.gateways.pendingSignUps().save(pending);
        this.gateways.notifier().sendValidation(new SignUpValidation(email, name, token));
    }

    // O e-mail já existir no provedor não é erro: é o caso que a validação por e-mail existe para
    // cobrir. Reaproveita a pessoa que está lá e não toca na senha dela — senão qualquer um
    // sobrescreveria a senha de outro se cadastrando com o e-mail dele.
    private UserId userIdFor(final Email email, final NewUser newUser) {
        return this.gateways
                .identityProvider()
                .userOfEmail(email)
                .map(it -> it.userId())
                .orElseGet(() -> this.gateways.identityProvider().create(newUser));
    }
}
