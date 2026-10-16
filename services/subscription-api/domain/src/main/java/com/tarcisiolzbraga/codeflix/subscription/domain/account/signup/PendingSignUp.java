package com.tarcisiolzbraga.codeflix.subscription.domain.account.signup;

import com.tarcisiolzbraga.codeflix.subscription.domain.AggregateRoot;
import com.tarcisiolzbraga.codeflix.subscription.domain.account.Account;
import com.tarcisiolzbraga.codeflix.subscription.domain.account.AccountId;
import com.tarcisiolzbraga.codeflix.subscription.domain.account.idp.UserId;
import com.tarcisiolzbraga.codeflix.subscription.domain.exceptions.ConflictException;
import com.tarcisiolzbraga.codeflix.subscription.domain.exceptions.DomainException;
import com.tarcisiolzbraga.codeflix.subscription.domain.person.Document;
import com.tarcisiolzbraga.codeflix.subscription.domain.person.Email;
import com.tarcisiolzbraga.codeflix.subscription.domain.person.Name;
import java.time.Instant;

// O cadastro que já existe no provedor de identidade e espera a pessoa confirmar o e-mail. É a peça
// que guarda o nome e o documento nesse meio-tempo, porque a conta só nasce na confirmação.
//
// Não guarda senha: ela foi direto ao provedor na hora do cadastro. E quando o e-mail já existia
// lá, nem isso aconteceu — ninguém sobrescreve a senha de outra pessoa se cadastrando com o e-mail
// dela.
//
// Guarda o **resumo** do token, nunca o token: ele é a credencial que vai no e-mail.
public class PendingSignUp extends AggregateRoot<PendingSignUpId> {

    private UserId userId;
    private Email email;
    private Name name;
    private Document document;
    private String tokenHash;
    private Instant expiresAt;
    private Instant confirmedAt;

    private PendingSignUp(
            final PendingSignUpId anId,
            final int version,
            final UserId anUserId,
            final Email anEmail,
            final Name aName,
            final Document aDocument,
            final String aTokenHash,
            final Instant expiresAt,
            final Instant confirmedAt) {
        super(anId, version);
        this.setUserId(anUserId);
        this.setEmail(anEmail);
        this.setName(aName);
        this.setDocument(aDocument);
        this.setTokenHash(aTokenHash);
        this.setExpiresAt(expiresAt);
        this.confirmedAt = confirmedAt;
    }

    public static PendingSignUp newPendingSignUp(
            final PendingSignUpId anId,
            final UserId anUserId,
            final Email anEmail,
            final Name aName,
            final Document aDocument,
            final SignUpToken aToken,
            final Instant expiresAt) {
        final var hash = aToken == null ? null : aToken.hashed();
        return new PendingSignUp(anId, 0, anUserId, anEmail, aName, aDocument, hash, expiresAt, null);
    }

    public static PendingSignUp with(
            final PendingSignUpId anId,
            final int version,
            final UserId anUserId,
            final Email anEmail,
            final Name aName,
            final Document aDocument,
            final String aTokenHash,
            final Instant expiresAt,
            final Instant confirmedAt) {
        return new PendingSignUp(
                anId, version, anUserId, anEmail, aName, aDocument, aTokenHash, expiresAt, confirmedAt);
    }

    // O prazo é regra de negócio, então quem decide se venceu é o agregado. A hora vem de fora: um
    // relógio consultado aqui dentro tornaria isto impossível de testar sem esperar.
    public void confirm(final Instant now) {
        this.assertArgumentNotNull(now, "'now' should not be null");
        if (isConfirmed()) {
            throw ConflictException.signUpAlreadyConfirmed();
        }
        if (this.expiresAt.isBefore(now)) {
            throw DomainException.with("the sign-up link expired; sign up again");
        }
        this.confirmedAt = now;
    }

    // A conta que este cadastro vira. Fica aqui, e não no caso de uso, para o mapeamento dos campos
    // morar junto de quem os guardou.
    public Account toAccount(final AccountId anAccountId) {
        return Account.newAccount(anAccountId, this.userId, this.email, this.name, this.document);
    }

    public boolean isConfirmed() {
        return this.confirmedAt != null;
    }

    public UserId userId() {
        return this.userId;
    }

    public Email email() {
        return this.email;
    }

    public Name name() {
        return this.name;
    }

    public Document document() {
        return this.document;
    }

    public String tokenHash() {
        return this.tokenHash;
    }

    public Instant expiresAt() {
        return this.expiresAt;
    }

    public Instant confirmedAt() {
        return this.confirmedAt;
    }

    private void setUserId(final UserId userId) {
        this.userId = this.assertArgumentNotNull(userId, "'userId' should not be null");
    }

    private void setEmail(final Email email) {
        this.email = this.assertArgumentNotNull(email, "'email' should not be null");
    }

    private void setName(final Name name) {
        this.name = this.assertArgumentNotNull(name, "'name' should not be null");
    }

    private void setDocument(final Document document) {
        this.document = this.assertArgumentNotNull(document, "'document' should not be null");
    }

    private void setTokenHash(final String tokenHash) {
        this.tokenHash = this.assertArgumentNotEmpty(tokenHash, "'tokenHash' should not be empty");
    }

    private void setExpiresAt(final Instant expiresAt) {
        this.expiresAt = this.assertArgumentNotNull(expiresAt, "'expiresAt' should not be null");
    }
}
