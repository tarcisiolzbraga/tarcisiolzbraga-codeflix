package com.tarcisiolzbraga.codeflix.admin.domain.exceptions;

import com.tarcisiolzbraga.codeflix.admin.domain.AggregateRoot;
import com.tarcisiolzbraga.codeflix.admin.domain.Identifier;
import java.util.List;

public class ConflictException extends DomainException {

    private static final long serialVersionUID = 1L;
    private static final String LINKED_TEMPLATE = "%s with ID %s is linked to at least one %s; deactivate it instead";

    private ConflictException(final String message) {
        super(message, List.of());
    }

    public static ConflictException linked(
            final Class<? extends AggregateRoot<?>> aggregate,
            final Identifier id,
            final Class<? extends AggregateRoot<?>> linkedTo) {
        return new ConflictException(
                LINKED_TEMPLATE.formatted(aggregate.getSimpleName(), id.getValue(), linkedTo.getSimpleName()));
    }
}
