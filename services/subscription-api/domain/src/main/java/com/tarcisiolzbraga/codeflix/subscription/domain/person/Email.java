package com.tarcisiolzbraga.codeflix.subscription.domain.person;

import com.tarcisiolzbraga.codeflix.subscription.domain.ValueObject;
import java.util.regex.Pattern;

// Mais fechado que o da referência, que usa ^(.+)@(\S+)$: aquele `.+` casa espaço, então
// "assi nante@codeflix.com" passava por ele.
//
// Não tenta validar e-mail pela RFC, que é caminho sem fim, nem exigir ponto no domínio, o que
// recusaria `usuario@localhost` — útil em desenvolvimento. Confere o que dá para afirmar sem
// adivinhar: nada em branco, um arroba só, os dois lados preenchidos e nenhum espaço. Quem diz de
// verdade se o endereço existe é a entrega da mensagem.
public record Email(String value) implements ValueObject {

    private static final int MAX_LENGTH = 255;
    private static final Pattern SHAPE = Pattern.compile("^[^\\s@]+@[^\\s@]+$");

    public Email {
        this.assertArgumentNotEmpty(value, "'email' should not be empty");
        this.assertArgumentMaxLength(
                value, MAX_LENGTH, "'email' should not have more than %d characters".formatted(MAX_LENGTH));
        this.assertConditionTrue(SHAPE.matcher(value).matches(), "'email' is invalid");
    }
}
