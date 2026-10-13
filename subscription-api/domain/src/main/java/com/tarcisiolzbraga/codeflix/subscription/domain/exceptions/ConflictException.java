package com.tarcisiolzbraga.codeflix.subscription.domain.exceptions;

import com.tarcisiolzbraga.codeflix.subscription.domain.AggregateRoot;
import com.tarcisiolzbraga.codeflix.subscription.domain.Identifier;

// Escrita que perdeu a corrida: alguém gravou o agregado entre a leitura e a gravação, e a cláusula
// do lock otimista não casou com nenhuma linha. É 409, não 404 — a linha existe, só não está na
// versão que quem escreve viu — e não é 422, porque o que o cliente mandou estava correto.
//
// A referência lança IllegalArgumentException nesse caso, que chegaria ao Spring como 500 e faria
// uma disputa de escrita parecer defeito do servidor.
public class ConflictException extends DomainException {

    private static final long serialVersionUID = 1L;
    private static final String MESSAGE_TEMPLATE = "%s with id %s was changed by someone else; read it again";

    private ConflictException(final String message) {
        super(message);
    }

    public static ConflictException staleVersion(
            final Class<? extends AggregateRoot<?>> aggregate, final Identifier id) {
        return new ConflictException(MESSAGE_TEMPLATE.formatted(aggregate.getSimpleName(), id.getValue()));
    }
}
