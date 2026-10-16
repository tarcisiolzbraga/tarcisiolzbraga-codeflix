package com.tarcisiolzbraga.codeflix.videos.domain;

import com.tarcisiolzbraga.codeflix.videos.domain.exceptions.DomainException;
import com.tarcisiolzbraga.codeflix.videos.domain.validation.ValidationError;
import java.util.Objects;
import java.util.UUID;

public interface Identifier {

    String getValue();

    // Converte no limite do domínio: id que vem de fora — evento de CDC, resposta da API do admin,
    // filtro de GraphQL — ou é UUID aqui, ou não entra. Aqui pesa mais que no admin, porque aqui
    // todo id vem de fora: nenhum é gerado neste serviço.
    //
    // Erro de domínio, nunca exceção de framework: o IllegalArgumentException do UUID escaparia
    // como falha genérica. Virando DomainException, o GraphQLExceptionResolver a traduz, e no
    // consumidor do CDC ela segue o caminho de qualquer mensagem que falha — tentativas e, no fim,
    // a fila morta, onde fica registrada em vez de virar dado ruim salvo em silêncio.
    static UUID uuidOf(final String value, final Class<? extends Identifier> type) {
        Objects.requireNonNull(value, "'value' should not be null");
        try {
            return UUID.fromString(value);
        } catch (final IllegalArgumentException exception) {
            throw DomainException.with(
                    new ValidationError("'%s' is not a valid %s".formatted(value, type.getSimpleName())));
        }
    }
}
