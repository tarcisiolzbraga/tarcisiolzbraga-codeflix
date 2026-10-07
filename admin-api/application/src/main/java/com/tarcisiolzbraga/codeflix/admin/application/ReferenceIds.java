package com.tarcisiolzbraga.codeflix.admin.application;

import com.tarcisiolzbraga.codeflix.admin.domain.exceptions.DomainException;
import com.tarcisiolzbraga.codeflix.admin.domain.validation.ValidationHandler;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Function;

// Converte os ids que o caso de uso recebe como texto, acumulando os malformados em vez de parar no
// primeiro. É a mesma razão do Notification: quem mandou a requisição recebe tudo o que está errado
// nela de uma vez, e não um erro por tentativa.
//
// O id recusado fica de fora do conjunto devolvido, e não entra como nulo: o passo seguinte
// pergunta ao banco quais existem, e não faz sentido perguntar por algo que não é id.
public final class ReferenceIds {

    private ReferenceIds() {
    }

    public static <ID> Set<ID> parse(
            final Set<String> values, final Function<String, ID> factory, final ValidationHandler handler) {
        final var ids = new LinkedHashSet<ID>();
        for (final var value : values) {
            try {
                ids.add(factory.apply(value));
            } catch (final DomainException exception) {
                exception.getErrors().forEach(handler::append);
            }
        }
        return Set.copyOf(ids);
    }
}
