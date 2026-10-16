package com.tarcisiolzbraga.codeflix.subscription.domain.account.signup;

import com.tarcisiolzbraga.codeflix.subscription.domain.ValueObject;
import com.tarcisiolzbraga.codeflix.subscription.domain.person.Email;
import com.tarcisiolzbraga.codeflix.subscription.domain.person.Name;

// O pedido de validação que sai do cadastro: para quem, como chamá-la, e o token que prova que ela
// leu a mensagem.
//
// Carrega o token, não um link: montar URL é assunto de quem sabe onde a aplicação está publicada, e
// o domínio não sabe nem deve.
public record SignUpValidation(Email to, Name name, SignUpToken token) implements ValueObject {

    public SignUpValidation {
        this.assertArgumentNotNull(to, "'to' should not be null");
        this.assertArgumentNotNull(name, "'name' should not be null");
        this.assertArgumentNotNull(token, "'token' should not be null");
    }
}
