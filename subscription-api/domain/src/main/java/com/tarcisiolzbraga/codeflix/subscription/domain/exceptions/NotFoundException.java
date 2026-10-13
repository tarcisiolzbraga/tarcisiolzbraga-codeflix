package com.tarcisiolzbraga.codeflix.subscription.domain.exceptions;

import com.tarcisiolzbraga.codeflix.subscription.domain.AggregateRoot;
import com.tarcisiolzbraga.codeflix.subscription.domain.Identifier;

// Agregado que não existe é 404, não 422: a requisição está bem formada, o recurso é que não está
// lá. A referência usa DomainException também para este caso, e por ela um plano inexistente
// responderia 422, como se o cliente tivesse mandado algo inválido.
//
// Estende DomainException para o handler poder tratar o caso geral numa cláusula só; quem decide o
// status é a ordem das cláusulas, da mais específica para a mais geral.
public class NotFoundException extends DomainException {

    private static final long serialVersionUID = 1L;
    private static final String MESSAGE_TEMPLATE = "%s with id %s was not found";

    private NotFoundException(final String message) {
        super(message);
    }

    public static NotFoundException with(final Class<? extends AggregateRoot<?>> aggregate, final Identifier id) {
        return new NotFoundException(MESSAGE_TEMPLATE.formatted(aggregate.getSimpleName(), id.getValue()));
    }
}
