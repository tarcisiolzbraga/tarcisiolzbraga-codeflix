package com.tarcisiolzbraga.codeflix.subscription.domain.account.idp;

import com.tarcisiolzbraga.codeflix.subscription.domain.ValueObject;
import com.tarcisiolzbraga.codeflix.subscription.domain.person.Email;
import com.tarcisiolzbraga.codeflix.subscription.domain.person.Name;

// O que se manda ao provedor para criar a pessoa. Separado do User, que é o que se lê de volta: a
// senha é dado de criação e nunca volta numa leitura. A referência junta os dois num tipo só, e por
// isso tem um campo de senha sempre nulo na leitura e obrigatório na escrita.
public record NewUser(Name name, Email email, String password) implements ValueObject {

    private static final int MIN_PASSWORD_LENGTH = 8;
    // 72 bytes é onde o bcrypt para de olhar; aceitar mais passaria uma confiança que não existe.
    private static final int MAX_PASSWORD_LENGTH = 72;

    public NewUser {
        this.assertArgumentNotNull(name, "'name' should not be null");
        this.assertArgumentNotNull(email, "'email' should not be null");
        this.assertArgumentNotEmpty(password, "'password' should not be empty");
        this.assertConditionTrue(
                password.length() >= MIN_PASSWORD_LENGTH,
                "'password' should have at least %d characters".formatted(MIN_PASSWORD_LENGTH));
        this.assertArgumentMaxLength(
                password,
                MAX_PASSWORD_LENGTH,
                "'password' should not have more than %d characters".formatted(MAX_PASSWORD_LENGTH));
    }
}
